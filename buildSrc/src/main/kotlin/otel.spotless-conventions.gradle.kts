plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        googleJavaFormat()
        licenseHeaderFile(rootProject.file("config/license/spotless.license.java"), "(package|import|public|class|// Includes work from:)")
    }

    format("misc") {
        // not using "**/..." to help keep spotless fast
        target(".gitignore", "*.md", "src/**/*.md", "*.sh", "src/**/*.properties")
        indentWithSpaces()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
