// Copyright 2018 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE.md file distributed with this work for terms.
package com.yahoo.luthier.webservice.data.config.metric;

import com.google.inject.Singleton;
import com.yahoo.bard.webservice.data.config.metric.makers.MetricMaker;
import com.yahoo.bard.webservice.data.dimension.DimensionDictionary;
import com.yahoo.bard.webservice.data.metric.MetricDictionary;
import com.yahoo.bard.webservice.data.time.DefaultTimeGrain;
import com.yahoo.bard.webservice.druid.model.postaggregation.ArithmeticPostAggregation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Metric maker dictionary.
 * <p>
 * map metric maker name to metric maker instance
 */
@Singleton
public class MetricMakerDictionary {

    private static final Logger LOG = LoggerFactory.getLogger(MetricMakerDictionary.class);

    /**
     * Maps from metric maker names to metric makers.
     */
    private final LinkedHashMap<String, MetricMaker> nameToMetricMaker;

    private MetricDictionary metricDictionary;
    private DimensionDictionary dimensionDictionary;

    /**
     * Constructor, initial map from all maker names to maker instances in dictionary.
     *
     * @param metricMakers        a list of metric makers
     * @param metricDictionary    metric dictionary as parameter for makers
     * @param dimensionDictionary dimension dictionary as parameter for makers
     */
    public MetricMakerDictionary(Set<MetricMakerTemplate> metricMakers,
                                 MetricDictionary metricDictionary,
                                 DimensionDictionary dimensionDictionary) {

        this.nameToMetricMaker = new LinkedHashMap<>();
        this.metricDictionary = metricDictionary;
        this.dimensionDictionary = dimensionDictionary;

        metricMakers.forEach(maker -> {

            try {
                Class<?> makerClass = Class.forName(maker.getClassPath());
                DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
                Constructor<?> selectedConstructor = null;

                // find appropriate constructor
                for (Constructor<?> candidateConstructor : makerClass.getDeclaredConstructors()) {
                    Class<?>[] pTypes = candidateConstructor.getParameterTypes();
                    String[] pNames = discoverer.getParameterNames(candidateConstructor);
                    int index = pTypes.length - 1;
                    while (index >= 0) {
                        if ("MetricDictionary".equals(pTypes[index].getSimpleName())
                                || "DimensionDictionary".equals(pTypes[index].getSimpleName())
                                || maker.getParams().containsKey(pNames[index])) {
                            index--;
                        } else {
                            break;
                        }
                    }
                    // if all parameters except MetricDictionary and DimensionDictionary
                    // in this candidate constructor appears in config file, set the selected constructor
                    // to this candidate constructor
                    // (have to make sure that constructor with more parameters be checked earlier)
                    if (index == -1) {
                        selectedConstructor = candidateConstructor;
                        break;
                    }
                }

                assert selectedConstructor != null;
                Class<?>[] paramsTypes = selectedConstructor.getParameterTypes();
                String[] paramsNames = discoverer.getParameterNames(selectedConstructor);

                Object[] args = IntStream.range(0, paramsTypes.length)
                        .mapToObj(i -> parseParams(paramsTypes[i].getSimpleName(), paramsNames[i], maker))
                        .toArray();

                add(maker.getName(), (MetricMaker) selectedConstructor.newInstance(args));

            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                LOG.error("The constructor of maker's class is inaccessible", e);
            } catch (InvocationTargetException e) {
                LOG.error(e.getCause().getMessage(), e);
            }
        });
    }

    /**
     * Find a metric maker given a metric maker Name.
     *
     * @param metricMakerName Name to search
     * @return the first metric maker found (if exists)
     */
    public MetricMaker findByName(String metricMakerName) {
        return nameToMetricMaker.get(metricMakerName.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Get all metric makers available in metric maker dictionary.
     *
     * @return a set of metric makers
     */
    public Set<MetricMaker> findAll() {
        return Collections.unmodifiableSet(new HashSet<>(nameToMetricMaker.values()));
    }

    /**
     * Adds the specified element to the dictionary if it is not already present.
     *
     * @param name        key to add to dictionary
     * @param metricMaker value to add to dictionary
     * @return <tt>true</tt> if the dictionary did not already contain the specified metric maker
     */
    public boolean add(String name, MetricMaker metricMaker) {
        if (nameToMetricMaker.containsKey(name.toLowerCase(Locale.ENGLISH))) {
            return false;
        }
        MetricMaker metricMakers = nameToMetricMaker.put(name.toLowerCase(Locale.ENGLISH), metricMaker);
        if (metricMakers != null) {
            // should never happen unless multiple loaders are running in race-condition
            ConcurrentModificationException e = new ConcurrentModificationException();
            LOG.error("Multiple loaders updating MetricMakerDictionary", e);
            throw e;
        }
        return true;
    }

    /**
     * Adds all of the metric makers in the specified collection to the dictionary.
     *
     * @param metricMakers collection of metric makers to add
     * @return <tt>true</tt> if the dictionary changed as a result of the call
     */
    public boolean addAll(Collection<MetricMaker> metricMakers) {
        boolean flag = false;
        for (MetricMaker metricMaker : metricMakers) {
            flag = add(metricMaker.toString(), metricMaker) || flag;
        }
        return flag;
    }

    @Override
    public String toString() {
        return "MetricMaker Dictionary: " + nameToMetricMaker;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nameToMetricMaker);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MetricMakerDictionary) {
            MetricMakerDictionary that = (MetricMakerDictionary) obj;
            return nameToMetricMaker.equals(that.nameToMetricMaker);
        }
        return false;
    }

    /**
     * Parse parameters for maker's constructor based on parameter's name and type.
     *
     * @param paramType type of the parameter in maker's constructor
     * @param paramName name of the parameter in maker's constructor
     * @param maker     the maker template used to find parameter's value by name
     * @return the value of parameter (can be any type)
     */
    private Object parseParams(String paramType, String paramName, MetricMakerTemplate maker) {
        if ("MetricDictionary".equals(paramType)) {
            return metricDictionary;
        }
        if ("DimensionDictionary".equals(paramType)) {
            return dimensionDictionary;
        }
        if ("ZonelessTimeGrain".equals(paramType)) {
            return DefaultTimeGrain.valueOf(maker.getParams().get(paramName));
        }
        if ("ArithmeticPostAggregationFunction".equals(paramType)) {
            return ArithmeticPostAggregation
                    .ArithmeticPostAggregationFunction
                    .valueOf(maker.getParams().get(paramName));
        }
        if ("boolean".equals(paramType)) {
            return Boolean.parseBoolean(maker.getParams().get(paramName));
        }
        if ("int".equals(paramType)) {
            return Integer.parseInt(maker.getParams().get(paramName));
        }
        if ("double".equals(paramType)) {
            return Double.parseDouble(maker.getParams().get(paramName));
        }
        return null;
    }
}