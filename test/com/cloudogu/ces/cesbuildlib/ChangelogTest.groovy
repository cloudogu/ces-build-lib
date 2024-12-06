package com.cloudogu.ces.cesbuildlib

import org.junit.Test
import static org.junit.Assert.*
import static groovy.test.GroovyAssert.shouldFail

class ChangelogTest {
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

    ScriptMock scriptMock = new ScriptMock()
    
    @Test
    void testGetCorrectVersion() {
        scriptMock.files.put('CHANGELOG.md', validChangelog)
        Changelog changelog = new Changelog(scriptMock)

        def changes1 = changelog.changesForVersion("v1.0.0")
        assertEquals("### Changed\\n- Something", changes1)

        def changes2 = changelog.changesForVersion("v0.9.9")
        assertEquals("### Added\\n- Anything", changes2)

        def changes3 = changelog.changesForVersion("v2.0.0")
        assertEquals("### Changed\\n- Everything!", changes3)
    }

    @Test
    void testWillWorkWithNewChangelog() {
        scriptMock.files.put('CHANGELOG.md', newChangelog)
        Changelog changelog = new Changelog(scriptMock)
        def changes = changelog.changesForVersion("v0.0.1")
        assertEquals("### Added\\n- Nothing yet", changes)
    }

    @Test
    void testReplaceInvalidCharactersCorrect() {
        scriptMock.files.put('CHANGELOG.md', validChangelog)
        Changelog changelog = new Changelog(scriptMock)

        assertEquals("", changelog.escapeForJson("\""))
        assertEquals("", changelog.escapeForJson("'"))
        assertEquals("", changelog.escapeForJson("''"))
        assertEquals("", changelog.escapeForJson("\\"))
        assertEquals("\\n", changelog.escapeForJson("\n"))
        assertEquals("\\n", changelog.escapeForJson("\n\"\"''\\\\"))
    }

    @Test
    void testThrowsErrorOnVersionNotFound() {
        scriptMock.files.put('CHANGELOG.md', validChangelog)
        Changelog changelog = new Changelog(scriptMock)
        def exception = shouldFail {
            changelog.changesForVersion("not existing version")
        }
        assertEquals("The desired version 'not existing version' could not be found in the changelog.", exception.getMessage())
    }
}
