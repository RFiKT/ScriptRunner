package jira.postfunctions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.atlassian.jira.user.DelegatingApplicationUser
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLinkTypeManager

//for testing in console
def issue = ComponentAccessor.issueManager.getIssueObject("JI-2")

//user for the script
def user = "auto"
def applicationUser = ComponentAccessor.getUserManager().getUserByKey(user)

def customFieldObjects = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
def externalPartnerValue = customFieldObjects.find {
    it.name == "External Partner"
}?.getValue(issue) as DelegatingApplicationUser
def epicIssue = customFieldObjects.find { it.name == "Epic Link" }?.getValue(issue) as Issue
def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager)
def epicLinkTypeId = issueLinkTypeManager.getIssueLinkTypes(false).find { it.name == "Epic-Story Link" }.id
def externalModellingIssue = ComponentAccessor.issueLinkManager.getOutwardLinks(epicIssue.id).findAll {
    it.linkTypeId == epicLinkTypeId
}.collect { it.destinationObject }.find { it.issueType.name == "External Modelling" }

//fields to copy
def productDataUrlField = customFieldObjects.find { it.name == "Product Data URL" } //text
def dimensionsField = customFieldObjects.find { it.name == "Dimensions" } //text
def geometryVariationsField = customFieldObjects.find { it.name == "Geometry Variations" } //text
def modelingComplexityField = customFieldObjects.find { it.name == "Modeling Complexity" } //select list
def modelingComplexityValue = issue.getCustomFieldValue(modelingComplexityField) as LazyLoadedOption

if (externalModellingIssue) {
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.setAssigneeId(externalPartnerValue.key)
            .addCustomFieldValue(productDataUrlField.idAsLong, productDataUrlField.getValue(issue).toString())
            .addCustomFieldValue(dimensionsField.idAsLong, dimensionsField.getValue(issue).toString())
            .addCustomFieldValue(geometryVariationsField.idAsLong, geometryVariationsField.getValue(issue).toString())
            .addCustomFieldValue(modelingComplexityField.idAsLong, modelingComplexityValue.optionId.toString())
            .setSkipScreenCheck(true)
    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(applicationUser,
            externalModellingIssue.getId(), issueInputParameters)
    if (validationResult.valid) issueService.update(applicationUser, validationResult)
}