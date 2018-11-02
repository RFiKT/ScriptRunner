package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.attachment.Attachment

def commentMgr = ComponentAccessor.commentManager
def issue = event.issue as MutableIssue

// gather the original author and comment body from the original issues comment
def type = issue.issueType.name
def newComment = event.comment
def originalAuthor = newComment.authorApplicationUser
def commentBody = newComment.body

// get original issue's linked issues and create comment on the linked issue
if (commentBody && type == "Service Request") {
        def attacher = ComponentAccessor.attachmentManager
        def linker = ComponentAccessor.issueLinkManager
        def sourceAttaches = attacher.getAttachments(issue)

        //iterate over the issues linked to the current issue through
        linker.getOutwardLinks(issue.id).each { outwardLink ->
                def destinationIssue = outwardLink.destinationObject
                //copy comment
                commentMgr.create(destinationIssue, originalAuthor, commentBody , true)
                //get attachments from linked issue
                def linkedAttaches = attacher.getAttachments(outwardLink.destinationObject)
                //check if this attachment from the linked issue already exists on the current issue
                //and if not exist copy it to the linked issue
                linkedAttaches.each { linkedAttach ->
                        if (!sourceAttaches.find { sourceAttach ->
                                        sourceAttach.filename == linkedAttach.filename &&
                                        sourceAttach.filesize == linkedAttach.filesize &&
                                        sourceAttach.mimetype == linkedAttach.mimetype
                        }) attacher.copyAttachment(linkedAttach, originalAuthor, destinationIssue)
                }
    }
}