plugins {
    id("fpgradle-minecraft") version ("0.9.0")
}

group = "invtweaks"

minecraft_fp {
    mod {
        modid = "inventorytweaks"
        name = "Inventory Tweaks"
        rootPkg = "$group"
    }

    api {
        packages = listOf("api")
    }

    core {
        coreModClass = "forge.asm.FMLPlugin"
        accessTransformerFile = "invtweaks_at.cfg"
    }

    tokens {
        tokenClass = "Tags"
    }

    publish {
        changelog = "https://github.com/GTMEGA/inventory-tweaks/releases/tag/$version"
        maven {
            repoUrl = "https://mvn.falsepattern.com/gtmega_releases/"
            repoName = "mega"
            group = "mega"
        }
    }
}