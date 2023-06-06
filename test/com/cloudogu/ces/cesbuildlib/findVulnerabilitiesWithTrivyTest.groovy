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

        def vulnerabilityList = script.call([imageName: 'alpine:3.17.2'])
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

        assert script.call([imageName: 'alpine:3.18.0']) == Collections.emptyList()
    }

    @Test
    void "error when args contain allowList"() {
        def errorMessage
        helper.registerAllowedMethod("error", [String.class], { arg ->
            throw new RuntimeException(arg)
        })
        
        try {
            script.call([imageName: 'alpine:3.18.0', allowList: ['CVE-2011-3374']])
        } catch(error) {
            errorMessage = error.message;
        }
       assert errorMessage == 'Arg allowList is deprecated, please use .trivyignore file'
    }


    // TODO test trivy version ??
    // TODO test severity ??


    String vulnerability1 = """
{
          "VulnerabilityID": "CVE-2023-0464",
          "PkgID": "libcrypto3@3.0.8-r0",
          "PkgName": "libcrypto3",
          "InstalledVersion": "3.0.8-r0",
          "FixedVersion": "3.0.8-r1",
          "Layer": {
            "DiffID": "sha256:7cd52847ad775a5ddc4b58326cf884beee34544296402c6292ed76474c686d39"
          },
          "SeveritySource": "nvd",
          "PrimaryURL": "https://avd.aquasec.com/nvd/cve-2023-0464",
          "DataSource": {
            "ID": "alpine",
            "Name": "Alpine Secdb",
            "URL": "https://secdb.alpinelinux.org/"
          },
          "Title": "Denial of service by excessive resource usage in verifying X509 policy constraints",
          "Description": "A security vulnerability has been identified in all supported versions of OpenSSL related to the verification of X.509 certificate chains that include policy constraints. Attackers may be able to exploit this vulnerability by creating a malicious certificate chain that triggers exponential use of computational resources, leading to a denial-of-service (DoS) attack on affected systems. Policy processing is disabled by default but can be enabled by passing the `-policy' argument to the command line utilities or by calling the `X509_VERIFY_PARAM_set1_policies()' function.",
          "Severity": "HIGH",
          "CweIDs": [
            "CWE-295"
          ],
          "CVSS": {
            "nvd": {
              "V3Vector": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H",
              "V3Score": 7.5
            },
            "redhat": {
              "V3Vector": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H",
              "V3Score": 5.9
            }
          },
          "References": [
            "https://access.redhat.com/security/cve/CVE-2023-0464",
            "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-0464",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=2017771e2db3e2b96f89bbe8766c3209f6a99545",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=2dcd4f1e3115f38cefa43e3efbe9b801c27e642e",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=879f7080d7e141f415c79eaa3a8ac4a3dad0348b",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=959c59c7a0164117e7f8366466a32bb1f8d77ff1",
            "https://nvd.nist.gov/vuln/detail/CVE-2023-0464",
            "https://ubuntu.com/security/notices/USN-6039-1",
            "https://www.cve.org/CVERecord?id=CVE-2023-0464",
            "https://www.openssl.org/news/secadv/20230322.txt"
          ],
          "PublishedDate": "2023-03-22T17:15:00Z",
          "LastModifiedDate": "2023-03-29T19:37:00Z"
        }
"""

    String vulnerability2 = """
{
          "VulnerabilityID": "CVE-2023-0464",
          "PkgID": "libssl3@3.0.8-r0",
          "PkgName": "libssl3",
          "InstalledVersion": "3.0.8-r0",
          "FixedVersion": "3.0.8-r1",
          "Layer": {
            "DiffID": "sha256:7cd52847ad775a5ddc4b58326cf884beee34544296402c6292ed76474c686d39"
          },
          "SeveritySource": "nvd",
          "PrimaryURL": "https://avd.aquasec.com/nvd/cve-2023-0464",
          "DataSource": {
            "ID": "alpine",
            "Name": "Alpine Secdb",
            "URL": "https://secdb.alpinelinux.org/"
          },
          "Title": "Denial of service by excessive resource usage in verifying X509 policy constraints",
          "Description": "A security vulnerability has been identified in all supported versions of OpenSSL related to the verification of X.509 certificate chains that include policy constraints. Attackers may be able to exploit this vulnerability by creating a malicious certificate chain that triggers exponential use of computational resources, leading to a denial-of-service (DoS) attack on affected systems. Policy processing is disabled by default but can be enabled by passing the `-policy' argument to the command line utilities or by calling the `X509_VERIFY_PARAM_set1_policies()' function.",
          "Severity": "HIGH",
          "CweIDs": [
            "CWE-295"
          ],
          "CVSS": {
            "nvd": {
              "V3Vector": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H",
              "V3Score": 7.5
            },
            "redhat": {
              "V3Vector": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H",
              "V3Score": 5.9
            }
          },
          "References": [
            "https://access.redhat.com/security/cve/CVE-2023-0464",
            "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-0464",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=2017771e2db3e2b96f89bbe8766c3209f6a99545",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=2dcd4f1e3115f38cefa43e3efbe9b801c27e642e",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=879f7080d7e141f415c79eaa3a8ac4a3dad0348b",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=959c59c7a0164117e7f8366466a32bb1f8d77ff1",
            "https://nvd.nist.gov/vuln/detail/CVE-2023-0464",
            "https://ubuntu.com/security/notices/USN-6039-1",
            "https://www.cve.org/CVERecord?id=CVE-2023-0464",
            "https://www.openssl.org/news/secadv/20230322.txt"
          ],
          "PublishedDate": "2023-03-22T17:15:00Z",
          "LastModifiedDate": "2023-03-29T19:37:00Z"
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
{
  "SchemaVersion": 2,
  "ArtifactName": "alpine:3.17.2",
  "ArtifactType": "container_image",
  "Metadata": {
    "OS": {
      "Family": "alpine",
      "Name": "3.17.2"
    },
    "ImageID": "sha256:b2aa39c304c27b96c1fef0c06bee651ac9241d49c4fe34381cab8453f9a89c7d",
    "DiffIDs": [
      "sha256:7cd52847ad775a5ddc4b58326cf884beee34544296402c6292ed76474c686d39"
    ],
    "RepoTags": [
      "alpine:3.17.2"
    ],
    "RepoDigests": [
      "alpine@sha256:ff6bdca1701f3a8a67e328815ff2346b0e4067d32ec36b7992c1fdc001dc8517"
    ],
    "ImageConfig": {
      "architecture": "amd64",
      "container": "4ad3f57821a165b2174de22a9710123f0d35e5884dca772295c6ebe85f74fe57",
      "created": "2023-02-11T04:46:42.558343068Z",
      "docker_version": "20.10.12",
      "history": [
        {
          "created": "2023-02-11T04:46:42.449083344Z",
          "created_by": "/bin/sh -c #(nop) ADD file:40887ab7c06977737e63c215c9bd297c0c74de8d12d16ebdf1c3d40ac392f62d in / "
        },
        {
          "created": "2023-02-11T04:46:42.558343068Z",
          "created_by": "/bin/sh -c #(nop)  CMD [\\"/bin/sh\\"]",
          "empty_layer": true
        }
      ],
      "os": "linux",
      "rootfs": {
        "type": "layers",
        "diff_ids": [
          "sha256:7cd52847ad775a5ddc4b58326cf884beee34544296402c6292ed76474c686d39"
        ]
      },
      "config": {
        "Cmd": [
          "/bin/sh"
        ],
        "Env": [
          "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        ],
        "Image": "sha256:ba2beca50019d79fb31b12c08f3786c5a0621017a3e95a72f2f8b832f894a427"
      }
    }
  },
  "Results": [
  {
      "Target": "alpine:3.17.2 (alpine 3.17.2)",
      "Class": "os-pkgs",
      "Type": "alpine",
      "Vulnerabilities":  ${expectedFullVulnerabilityList}
  }
]
}
"""

    String trivyOutputWithoutVulns = """
{
  "SchemaVersion": 2,
  "ArtifactName": "alpine:3.18.0",
  "ArtifactType": "container_image",
  "Metadata": {
    "OS": {
      "Family": "alpine",
      "Name": "3.18.0"
    },
    "ImageID": "sha256:5e2b554c1c45d22c9d1aa836828828e320a26011b76c08631ac896cbc3625e3e",
    "DiffIDs": [
      "sha256:bb01bd7e32b58b6694c8c3622c230171f1cec24001a82068a8d30d338f420d6c"
    ],
    "RepoTags": [
      "alpine:3.18.0"
    ],
    "RepoDigests": [
      "alpine@sha256:02bb6f428431fbc2809c5d1b41eab5a68350194fb508869a33cb1af4444c9b11"
    ],
    "ImageConfig": {
      "architecture": "amd64",
      "container": "0aa41c99f485b1dbe59101f2bb8e6922d9bf7cc1745f1c768f988b1bd724f11a",
      "created": "2023-05-09T23:11:10.132147526Z",
      "docker_version": "20.10.23",
      "history": [
        {
          "created": "2023-05-09T23:11:10.007217553Z",
          "created_by": "/bin/sh -c #(nop) ADD file:7625ddfd589fb824ee39f1b1eb387b98f3676420ff52f26eb9d975151e889667 in / "
        },
        {
          "created": "2023-05-09T23:11:10.132147526Z",
          "created_by": "/bin/sh -c #(nop)  CMD [\\"/bin/sh\\"]",
          "empty_layer": true
        }
      ],
      "os": "linux",
      "rootfs": {
        "type": "layers",
        "diff_ids": [
          "sha256:bb01bd7e32b58b6694c8c3622c230171f1cec24001a82068a8d30d338f420d6c"
        ]
      },
      "config": {
        "Cmd": [
          "/bin/sh"
        ],
        "Env": [
          "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        ],
        "Image": "sha256:fa9de512065d701938f44d4776827d838440ed00f1f51b1fff5f97f7378acf08"
      }
    }
  },
  "Results": [
    {
      "Target": "alpine:3.18.0 (alpine 3.18.0)",
      "Class": "os-pkgs",
      "Type": "alpine"
    }
  ]
}
"""
}
