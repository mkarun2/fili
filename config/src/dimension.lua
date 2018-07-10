--
-- Created by IntelliJ IDEA.
-- User: ylin08
-- Date: 7/3/18
-- Time: 3:20 PM
-- To change this template use File | Settings | File Templates.
--

local utils = require("src/dimensionUtils")
local parser = require("src/jsonParser")

local M = {
    dimensions = {}
}

DEFAULT = {
    CATEGORY = "General"
}

-- fieldSet name = {pk(field as primary key), f(other fields)}
FIELDSETS = {
    DEFAULT = { pk "ID", f "DESC" },
    COUNTRY = { pk "ID", f("DESC", "COUNTY", "STATE") },
    PAGE = { pk "ID", f "DESC" }
}

-- dimension name = {longName, description, fields, categories}
DIMENSIONS = {
    COMMENT = {"wiki comment", "Comment for the edit to the wiki page", FIELDSETS.DEFAULT, nil},
    COUNTRY_ISO_CODE = { "wiki countryIsoCode", "Iso Code of the country to which the wiki page belongs", FIELDSETS.COUNTRY, DEFAULT.CATEGORY },
    REGION_ISO_CODE = { "wiki regionIsoCode", "Iso Code of the region to which the wiki page belongs", FIELDSETS.DEFAULT, nil },
    PAGE = { "wiki page", "Page is a document that is suitable for World Wide Web and web browsers", FIELDSETS.PAGE, nil },
    USER = { "wiki user", "User is a person who generally use or own wiki services", { pk "ID", f("DESC", "AGE", "SEX") }, nil },
    IS_UNPATROLLED = {"wiki isUnpatrolled", "Unpatrolled are class of pages that are not been patrolled", FIELDSETS.DEFAULT, nil},
    IS_NEW = {"wiki isNew", "New Page is the first page that is created in wiki", FIELDSETS.DEFAULT, nil},
    IS_ROBOT = {"wiki isRobot", "Robot is an tool that carries out repetitive and mundane tasks", FIELDSETS.DEFAULT, nil},
    IS_ANONYMOUS = {"wiki isAnonymous", "Anonymous are individual or entity whose identity is unknown", FIELDSETS.DEFAULT, nil},
    IS_MINOR = {"wiki isMinor", "Minor is a person who is legally considered a minor", FIELDSETS.DEFAULT, nil},
    NAMESPACE = {"wiki namespace", "Namespace is a set of wiki pages that begins with a reserved word", FIELDSETS.DEFAULT, nil},
    CHANNEL = {"wiki channel", "Channel is a set of wiki pages on a certain channel", FIELDSETS.DEFAULT, nil},
    COUNTRY_NAME = {"wiki countryName", "Name of the Country to which the wiki page belongs", FIELDSETS.DEFAULT, nil},
    REGION_NAME = {"wiki regionName", "Name of the Region to which the wiki page belongs", FIELDSETS.DEFAULT, nil},
    METRO_CODE = {"wiki metroCode", "Metro Code to which the wiki page belongs", FIELDSETS.DEFAULT, nil},
    CITY_NAME = {"wiki cityName", "Name of the City to which the wiki page belongs", FIELDSETS.DEFAULT, nil}
}

utils.add_dimensions(DIMENSIONS, M.dimensions)
parser.save("DimensionConfigTemplate.json", M)

return M