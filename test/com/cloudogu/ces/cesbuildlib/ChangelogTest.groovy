package com.cloudogu.ces.cesbuildlib

import org.junit.Test

class ChangelogTest extends GroovyTestCase {
    def validChangelog =
            '''
## [Unreleased]
### Changed
- Some other things

## [v2.0.0] - 2020-01-01
### Changed
- Everything!
 
## [v1.0.0] - 2020-01-01
### Changed
- Something
    
## [v0.9.9] - 2020-01-01
### Added
- Anything
    
'''
    def newChangelog =
            '''
## [Unreleased]

## [v0.0.1] - 2020-01-01
### Added
- Nothing yet
    
'''

    @Test
    void testGetCorrectVersion() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat CHANGELOG.md", validChangelog)
        Changelog changelog = new Changelog(scriptMock)

        def changes1 = changelog.changesForVersion("v1.0.0")
        assertEquals("### Changed\\n- Something", changes1)

        def changes2 = changelog.changesForVersion("v0.9.9")
        assertEquals("### Added\\n- Anything", changes2)

        def changes3 = changelog.changesForVersion("v2.0.0")
        assertEquals("### Changed\\n- Everything!", changes3)

        assertEquals(13, scriptMock.allActualArgs.size())
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[0])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[1])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[2])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[3])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[4])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[5])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[6])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[7])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[8])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[9])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[10])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[11])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[12])
    }

    @Test
    void testWillWorkWithNewChangelog() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat CHANGELOG.md", newChangelog)
        Changelog changelog = new Changelog(scriptMock)
        def changes = changelog.changesForVersion("v0.0.1")
        assertEquals("### Added\\n- Nothing yet", changes)
    }

    @Test
    void testReplaceInvalidCharactersCorrect() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat CHANGELOG.md", validChangelog)
        Changelog changelog = new Changelog(scriptMock)

        assertEquals("", changelog.formatForJson("\""))
        assertEquals("", changelog.formatForJson("'"))
        assertEquals("", changelog.formatForJson("''"))
        assertEquals("", changelog.formatForJson("\\"))
        assertEquals("\\n", changelog.formatForJson("\n"))
        assertEquals("\\n", changelog.formatForJson("\n\"\"''\\\\"))
    }

    @Test
    void testThrowsErrorOnVersionNotFound() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat CHANGELOG.md", newChangelog)
        Changelog changelog = new Changelog(scriptMock)
        def exception = shouldFail {
            changelog.changesForVersion("not existing version")
        }
        assertEquals("The desired version 'not existing version' could not be found in the changelog.", exception)
    }
}
