plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11" /* DO NOT EDIT */

stonecutter parameters {
    swaps["mod_id"] = "\"${property("mod.id")}\";"
    swaps["mod_name"] = "\"${property("mod.name")}\";"
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
