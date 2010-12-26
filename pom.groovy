project {
    modelVersion '4.0.0'
    groupId 'com.chocolatey.pmsencoder'
    artifactId 'pmsencoder'
    version '1.2.5'
    name 'PMSEncoder'
    description 'A plugin for PS3 Media Server that adds support for scriptable web video streaming.'

    properties {
        'project.build.sourceEncoding' 'UTF-8'
        'groovypp-version' '0.4.113'
        'groovypp-repo' 'http://groovypp.artifactoryonline.com/groovypp'
        'junit-version' '4.8.2'
    }

    repositories {
        repository {
            id 'libs-releases'
            url '${groovypp-repo}/libs-releases'
        }

        repository {
            id 'libs-snapshots'
            url '${groovypp-repo}/libs-snapshots'
        }

        repository {
            snapshots {
                enabled 'false'
            }
            id 'java.net'
            name 'Java.net Repository'
            url 'http://download.java.net/maven/2/'
        }

        repository {
            snapshots {
                enabled 'false'
            }
            id 'offical'
            name 'Maven Official Repository'
            url 'http://repo1.maven.org/maven2'
        }
    }

    pluginRepositories {
        pluginRepository {
            id 'plugins-releases'
            url '${groovypp-repo}/plugins-releases'
        }

        pluginRepository {
            id 'plugins-snapshots'
            url '${groovypp-repo}/plugins-snapshots'
        }

        pluginRepository {
            id 'ossrh'
            name 'Sonatype OSS Repository'
            url 'http://oss.sonatype.org/content/groups/public'
        }
    }

    dependencies {
        dependency {
            groupId 'org.mbte.groovypp'
            artifactId 'groovypp-all'
            version '${groovypp-version}'
            scope 'compile'
        }

        dependency {
            groupId 'org.codehaus.groovy.modules.http-builder'
            artifactId 'http-builder'
            version '0.5.1'
            scope 'compile'
        }

        dependency {
            groupId 'log4j'
            artifactId 'log4j'
            version '1.2.16'
        }

        dependency {
            groupId 'mockit'
            artifactId 'jmockit'
            version '0.999.4'
            scope 'test'
        }

        dependency {
            groupId 'junit'
            artifactId 'junit'
            version '${junit-version}'
            scope 'test'
        }

        dependency {
            groupId 'excalibur'
            artifactId 'excalibur-fortress'
            version '1.0'
        }

        // non-Mavenized dependencies installed via maven-external-dependency-plugin

        dependency {
            groupId 'com.google.code'
            artifactId 'ps3mediaserver'
            version '1.20.412'
            scope 'provided'
        }

        dependency {
            groupId 'info.codesaway'
            artifactId 'regexplus'
            version '2010-04-16' // server Last-modified header for RegExPlus.jar
        }
    }

    build {
        sourceDirectory '${project.basedir}/src/main/groovy'
        testSourceDirectory '${project.basedir}/src/test/groovy'
        defaultGoal 'surefire-report:report'
        plugins {
            // in the absence of a super-POM
            plugin {
                artifactId 'maven-compiler-plugin'
                version '2.3.2'
                configuration {
                    source '1.6'
                    target '1.6'
                }
            }

            plugin {
                groupId 'org.codehaus.gmaven'
                artifactId 'gmaven-plugin'
                version '1.3'
                executions {
                    execution {
                        goals {
                            goal 'generateStubs'
                            goal 'compile'
                            goal 'generateTestStubs'
                            goal 'testCompile'
                        }
                    }
                }

                dependencies {
                    dependency {
                        groupId 'org.codehaus.gmaven.runtime'
                        artifactId 'gmaven-runtime-1.7'
                        version '1.3'
                        exclusions {
                            exclusion {
                                artifactId 'groovy-all'
                                groupId 'org.codehaus.groovy'
                            }
                            exclusion {
                                artifactId 'junit'
                                groupId 'junit'
                            }
                        }
                    }

                    dependency {
                        groupId 'org.mbte.groovypp'
                        artifactId 'groovypp'
                        version '${groovypp-version}'
                    }

                    /*
                        in conjunction with the exclusion above and the dependency below,
                        this ensures we have have a consistent dependency on the same version
                        of junit across different libraries
                    */
                    dependency {
                        groupId 'junit'
                        artifactId 'junit'
                        version '${junit-version}'
                    }
                }

                configuration {
                    providerSelection '1.7'
                    verbose 'true'
                    debug 'true'
                    stacktrace 'true'
                }
            }

            /*
               FIXME: 2.2 constantly breaks the build (via plexus-archiver)
               downgrade to 2.1 until 2.3 is released
            */
            plugin {
                artifactId 'maven-assembly-plugin'
                version '2.1'
                configuration {
                    descriptorRefs {
                        descriptorRef 'jar-with-dependencies'
                    }
                }
            }

            plugin {
                groupId 'com.savage7.maven.plugins'
                artifactId 'maven-external-dependency-plugin'
                version '0.5-SNAPSHOT'

                executions {
                    execution {
                        id 'clean-external-dependencies'
                        phase 'clean'
                        goals {
                            // pmvn com.savage7.maven.plugins:maven-external-dependency-plugin:clean-external
                            goal 'clean-external'
                        }
                    }
                    execution {
                        id 'resolve-install-external-dependencies'
                        phase 'process-resources'
                        goals {
                            // pmvn com.savage7.maven.plugins:maven-external-dependency-plugin:resolve-external
                            goal 'resolve-external'
                            // pmvn com.savage7.maven.plugins:maven-external-dependency-plugin:install-external
                            goal 'install-external'
                        }
                    }
                }

                inherited 'false'

                configuration {
                    stagingDirectory '${project.build.directory}/dependencies/'
                    createChecksum 'true'
                    skipChecksumVerification 'false'
                    force 'false'
                    artifactItems {
                        artifactItem {
                            groupId 'info.codesaway'
                            artifactId 'regexplus'
                            version '2010-04-16' // server Last-modified header for RegExPlus.jar
                            packaging 'jar'
                            downloadUrl 'http://codesaway.info/RegExPlus/RegExPlus.jar'
                            install 'true'
                            force 'false'
                            checksum 'b31c60f99a3c163af1658e6d894f579af98011d6'
                        }
                        artifactItem {
                            groupId 'com.google.code'
                            artifactId 'ps3mediaserver'
                            version '1.20.412'
                            packaging 'jar'
                            downloadUrl 'http://ps3mediaserver.googlecode.com/files/pms-generic-linux-unix-1.20.412.tgz'
                            install 'true'
                            force 'false'
                            checksum 'c7b609a27aeab358e870b5c490425206e067bd01'
                            extractFile 'pms-linux-1.20.412/pms.jar'
                            extractFileChecksum '4644fd515cb9c4f9a24e7629ecbca0626b961f45'
                        }
                    }
                }
            }
        }
    }

    reporting {
        plugins {
            plugin {
                artifactId 'maven-surefire-report-plugin'
                version '2.7'
                configuration {
                    showSuccess 'false'
                }
            }
        }
    }
}
