package com.cloudogu.ces.cesbuildlib

import com.lesfurets.jenkins.unit.BasePipelineTest
import groovy.json.JsonSlurper
import org.junit.After
import org.junit.Before
import org.junit.Test 

class FindVulnerabilitiesWithTrivyTest extends BasePipelineTest {

    def script
    def slurper = new JsonSlurper()

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        script = loadScript('vars/findVulnerabilitiesWithTrivy.groovy')
        script.env = [WORKSPACE: '/tmp/workspace']
        Docker.metaClass.constructor = { def script -> DockerMock.create() }
    }

    @After
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        Docker.metaClass = null
    }

    @Test
    void "return a list of vulnerabilities"() {
        helper.registerAllowedMethod("readJSON", [Map.class], { map ->
            return slurper.parseText(trivyOutputWithVulns)
        })

        def vulnerabilityList = script.call([imageName: 'nginx'])
        assert vulnerabilityList == slurper.parseText(expectedFullVulnerabilityList)
        assert vulnerabilityList.size() == 2
    }

    @Test
    void "return false if imageName is not passed"() {
        assert script.validateArgs() == false
    }

    @Test
    void "return false if imageName is emptyString"() {
        assert script.validateArgs([imageName: '']) == false
    }

    @Test
    void "return empty list if no imageName is passed"() {
        assert script.call() == Collections.emptyList()
    }

    @Test
    void "return empty list if imageName is emptyString"() {
        assert script.call([imageName: '']) == Collections.emptyList()
    }

    @Test
    void "return empty list when no vulnerabilities"() {
        helper.registerAllowedMethod("readJSON", [Map.class], { map ->
            return slurper.parseText(trivyOutputWithoutVulns)
        })

        assert script.call([imageName: 'nginx']) == Collections.emptyList()
    }

    @Test
    void "only single vulnerability when allowList matches vulnerability1"() {
        helper.registerAllowedMethod("readJSON", [Map.class], { map ->
            return slurper.parseText(trivyOutputWithVulns)
        })

        def vulnerabilityList = script.call([imageName: 'nginx', allowList: ['CVE-2011-3374']])
        assert vulnerabilityList == slurper.parseText(expectedVulnerability2List)
        assert vulnerabilityList.size() == 1
    }

    @Test
    void "only single vulnerability when allowList matches vulnerability2"() {
        helper.registerAllowedMethod("readJSON", [Map.class], { map ->
            return slurper.parseText(trivyOutputWithVulns)
        })

        def vulnerabilityList = script.call([imageName: 'nginx', allowList: ['CVE-2019-18276']])
        assert vulnerabilityList == slurper.parseText(expectedVulnerability1List)
        assert vulnerabilityList.size() == 1
    }

    @Test
    void "return empty list when both vulnerabilites are on allow list"() {
        helper.registerAllowedMethod("readJSON", [Map.class], { map ->
            return slurper.parseText(trivyOutputWithVulns)
        })

        assert script.call([imageName: 'nginx', allowList: ['CVE-2019-18276', 'CVE-2011-3374']]) == Collections.emptyList()
    }

    @Test
    void "full vulnerability list when allowList does not match"() {
        helper.registerAllowedMethod("readJSON", [Map.class], { map ->
            return slurper.parseText(trivyOutputWithVulns)
        })

        def vulnerabilityList = script.call([imageName: 'nginx', allowList: ['CVE-2011-3371']])
        assert vulnerabilityList == slurper.parseText(expectedFullVulnerabilityList)
        assert vulnerabilityList.size() == 2
    }

    // TODO test trivy version ??
    // TODO test severity ??


    String vulnerability1 = """
      {
        "VulnerabilityID": "CVE-2011-3374",
        "PkgName": "apt",
        "InstalledVersion": "1.8.2.2",
        "Layer": {
          "Digest": "sha256:a076a628af6f7dcabc536bee373c0d9b48d9f0516788e64080c4e841746e6ce6",
          "DiffID": "sha256:cb42413394c4059335228c137fe884ff3ab8946a014014309676c25e3ac86864"
        },
        "SeveritySource": "debian",
        "PrimaryURL": "https://avd.aquasec.com/nvd/cve-2011-3374",
        "Description": "It was found that apt-key in apt, all versions, do not correctly validate gpg keys with the master keyring, leading to a potential man-in-the-middle attack.",
        "Severity": "LOW",
        "CweIDs": [
          "CWE-347"
        ],
        "CVSS": {
          "nvd": {
            "V2Vector": "AV:N/AC:M/Au:N/C:N/I:P/A:N",
            "V3Vector": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:L/A:N",
            "V2Score": 4.3,
            "V3Score": 3.7
          }
        },
        "References": [
          "https://access.redhat.com/security/cve/cve-2011-3374",
          "https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=642480",
          "https://people.canonical.com/~ubuntu-security/cve/2011/CVE-2011-3374.html",
          "https://security-tracker.debian.org/tracker/CVE-2011-3374",
          "https://snyk.io/vuln/SNYK-LINUX-APT-116518"
        ],
        "PublishedDate": "2019-11-26T00:15:00Z",
        "LastModifiedDate": "2019-12-04T15:35:00Z"
      }
"""

    String vulnerability2 = """
      {
        "VulnerabilityID": "CVE-2019-18276",
        "PkgName": "bash",
        "InstalledVersion": "5.0-4",
        "Layer": {
          "Digest": "sha256:a076a628af6f7dcabc536bee373c0d9b48d9f0516788e64080c4e841746e6ce6",
          "DiffID": "sha256:cb42413394c4059335228c137fe884ff3ab8946a014014309676c25e3ac86864"
        },
        "SeveritySource": "debian",
        "PrimaryURL": "https://avd.aquasec.com/nvd/cve-2019-18276",
        "Title": "bash: when effective UID is not equal to its real UID the saved UID is not dropped",
        "Description": "An issue was discovered in disable_priv_mode in shell.c in GNU Bash through 5.0 patch 11. By default, if Bash is run with its effective UID not equal to its real UID, it will drop privileges by setting its effective UID to its real UID. However, it does so incorrectly. On Linux and other systems that support \\"saved UID\\" functionality, the saved UID is not dropped. An attacker with command execution in the shell can use \\"enable -f\\" for runtime loading of a new builtin, which can be a shared object that calls setuid() and therefore regains privileges. However, binaries running with an effective UID of 0 are unaffected.",
        "Severity": "HIGH",
        "CweIDs": [
          "CWE-273"
        ],
        "CVSS": {
          "nvd": {
            "V2Vector": "AV:L/AC:L/Au:N/C:C/I:C/A:C",
            "V3Vector": "CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H",
            "V2Score": 7.2,
            "V3Score": 7.8
          },
          "redhat": {
            "V3Vector": "CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H",
            "V3Score": 7.8
          }
        },
        "References": [
          "http://packetstormsecurity.com/files/155498/Bash-5.0-Patch-11-Privilege-Escalation.html",
          "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2019-18276",
          "https://github.com/bminor/bash/commit/951bdaad7a18cc0dc1036bba86b18b90874d39ff",
          "https://security.netapp.com/advisory/ntap-20200430-0003/",
          "https://www.youtube.com/watch?v=-wGtxJ8opa8"
        ],
        "PublishedDate": "2019-11-28T01:15:00Z",
        "LastModifiedDate": "2020-04-30T19:15:00Z"
      }
"""

    String expectedFullVulnerabilityList = """
[
        ${vulnerability1},
        ${vulnerability2}
      
    ]
"""

    String expectedVulnerability1List = """
[
        ${vulnerability1}      
    ]
"""

    String expectedVulnerability2List = """
[
        ${vulnerability2}      
    ]
"""

    String trivyOutputWithVulns = """
[
  {
    "Target": "nginx (debian 10.7)",
    "Type": "debian",
    "Vulnerabilities": ${expectedFullVulnerabilityList}
  }
]
"""

    String trivyOutputWithoutVulns = """
[
  {
    "Target": "nginx (debian 10.7)",
    "Type": "debian",
    "Vulnerabilities": null
  }
]
"""
}
