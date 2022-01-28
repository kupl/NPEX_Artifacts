<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!-- $Id$ -->
<!-- This stylesheet extracts the checks from the testcase so the list of checks can be built in Java code. -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">

<xsl:variable name="basic-checks" select="document('basic-checks.xml')/checks/*" />

<xsl:template match="testcase">
  <checks>
    <xsl:apply-templates select="checks"/>
    <xsl:apply-templates select="if-checks"/>
    <xsl:apply-templates select="event-checks"/>
  </checks>
</xsl:template>

<xsl:template match="checks">
  <at-checks>
    <xsl:copy-of select="$basic-checks" />
    <xsl:copy-of select="*" />
  </at-checks>
</xsl:template>

<xsl:template match="if-checks">
  <if-checks>
    <xsl:copy-of select="*"/>
  </if-checks>
</xsl:template>

<xsl:template match="event-checks">
  <event-checks>
    <xsl:copy-of select="*"/>
  </event-checks>
</xsl:template>

<xsl:template match="text()" />

</xsl:stylesheet>
