# zip4j

This repository contains decompiled source code for the zip4j library versions 1.3.2 (vulnerable) and 1.3.3 (fixed) for CVE-2018-1002202, for the CWE-Bench-Java dataset. The original zip4j repository on Github doesn't include code from these versions.

## Versions

  ### Version 1.3.2 (Vulnerable)
  - **Tag**: `1.3.2`
  - **Status**: Contains path traversal vulnerability
  - **Source**: Decompiled from `zip4j-1.3.2.jar` (Maven Central)
  - **Buggy Commit**: `b844e4a2d0e02bacb8ab1c2001e7c3c7f4b35c9a`
  - [1.3.2 jar download page](https://mvnrepository.com/artifact/net.lingala.zip4j/zip4j/1.3.2)

  ### Version 1.3.3 (Fixed)
  - **Tag**: `1.3.3`
  - **Status**: Vulnerability patched
  - **Source**: Decompiled from `zip4j-1.3.3.jar` (Maven Central)
  - **Fixed Commit**: `cbabdcf15842de7e1ec66f20aa2163e95ed13a79`
  - [1.3.3 jar download page](https://mvnrepository.com/artifact/net.lingala.zip4j/zip4j/1.3.3)

## Original Project

  - Original Author: Srikanth Lingala
  - [Original Repository](https://github.com/srikanth-lingala/zip4j)
