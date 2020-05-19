package com.cloudogu.ces.cesbuildlib

import org.junit.Test

class ChangelogParserTest extends GroovyTestCase {
    def validChangelog =
'''
## [Unreleased]
### Changed
- Some other things

## [v2.0.0]
### Changed
- Everything!
 
## [v1.0.0]
### Changed
- Something
    
## [v0.9.9]
### Added
- Anything
    
'''

    @Test
    void testGetCorrectVersion() {
        ScriptMock scriptMock = new ScriptMock()
        ChangelogParser changelog = new ChangelogParser(scriptMock, new ChangelogMock(validChangelog))

        def changes1 = changelog.getChangesForVersion("v1.0.0")
        assertEquals("## [v1.0.0]\\n### Changed\\n- Something", changes1)

        def changes2 = changelog.getChangesForVersion("v0.9.9")
        assertEquals("## [v0.9.9]\\n### Added\\n- Anything", changes2)

        def changes3 = changelog.getChangesForVersion("v2.0.0")
        assertEquals("## [v2.0.0]\\n### Changed\\n- Everything!", changes3)
    }

    @Test
    void testReplaceInvalidCharactersCorrect(){
        ScriptMock scriptMock = new ScriptMock()
        ChangelogParser changelog = new ChangelogParser(scriptMock, new ChangelogMock(""))

        assertEquals("", changelog.formatForJson("\""))
        assertEquals("", changelog.formatForJson("'"))
        assertEquals("", changelog.formatForJson("''"))
        assertEquals("", changelog.formatForJson("\\"))
        assertEquals("\\n", changelog.formatForJson("\n"))
        assertEquals("\\n", changelog.formatForJson("\n\"\"''\\\\"))
    }
}
