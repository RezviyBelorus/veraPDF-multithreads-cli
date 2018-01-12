<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/">
        <xsl:copy-of select="/report/jobs/job"/>
    </xsl:template>

</xsl:stylesheet>