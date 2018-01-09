<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output indent="yes" method="xml"/>
    <xsl:strip-space elements="*"/>

    <xsl:param name="filePath"/>
    <xsl:param name="file" select="document($filePath)"/>
    <xsl:param name="start" select="default"/>
    <xsl:param name="finish" select="default"/>
    <xsl:param name="duration" select="default"/>

    <xsl:variable name="updateItems" select="$file/report/jobs/job"/>
    <xsl:variable name="batchSummary" select="$file/report/batchSummary"/>


    <xsl:template match="@*|node()" name="identity">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>

    </xsl:template>

    <xsl:template match="/report/jobs">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()[not(self::entry)]"/>
            <xsl:apply-templates select="$updateItems"/>
        </xsl:copy>
    </xsl:template>


    <xsl:template match="/report/batchSummary">
        <batchSummary>
            <xsl:attribute name="totalJobs">
                <xsl:value-of select="sum(//@totalJobs)+sum($file/report/batchSummary/@totalJobs)"/>
            </xsl:attribute>
            <xsl:attribute name="failedToParse">
                <xsl:value-of select="sum(//@failedToParse)+sum($file/report/batchSummary/@failedToParse)"/>
            </xsl:attribute>
            <xsl:attribute name="encrypted">
                <xsl:value-of select="sum(//@encrypted)+sum($file/report/batchSummary/@encrypted)"/>
            </xsl:attribute>

            <validationReports>
                <xsl:attribute name="compliant">
                    <xsl:value-of
                            select="sum(//@compliant)+sum($file/report/batchSummary/validationReports/@compliant)"/>
                </xsl:attribute>
                <xsl:attribute name="nonCompliant">
                    <xsl:value-of
                            select="sum(//@nonCompliant)+sum($file/report/batchSummary/validationReports/@nonCompliant)"/>
                </xsl:attribute>
                <xsl:attribute name="failedJobs">
                    <xsl:value-of
                            select="sum(/report/batchSummary/validationReports/@failedJobs)+sum($file/report/batchSummary/validationReports/@failedJobs)"/>
                </xsl:attribute>
                <xsl:value-of select="sum(//validationReports) + sum($file/report/batchSummary/validationReports)"/>
            </validationReports>

            <featureReports>
                <xsl:attribute name="failedJobs">
                    <xsl:value-of
                            select="sum(/report/batchSummary/featureReports/@failedJobs)+sum($file/report/batchSummary/featureReports/@failedJobs)"/>
                </xsl:attribute>
                <xsl:value-of
                        select="sum(/report/batchSummary/featureReports) + sum($file/report/batchSummary/featureReports)"/>
            </featureReports>

            <repairReports>
                <xsl:attribute name="failedJobs">
                    <xsl:value-of
                            select="sum(/report/batchSummary/repairReports/@failedJobs)+sum($file/report/batchSummary/repairReports/@failedJobs)"/>
                </xsl:attribute>
                <xsl:value-of select="sum(//repairReports) + sum($file/report/batchSummary/repairReports)"/>
            </repairReports>
            <duration>
                <xsl:attribute name="start">
                    <xsl:value-of select="$start"/>
                </xsl:attribute>
                <xsl:attribute name="finish">
                    <xsl:value-of select="$finish"/>
                </xsl:attribute>
                <xsl:value-of select="$duration"/>
            </duration>
        </batchSummary>
    </xsl:template>
</xsl:stylesheet>