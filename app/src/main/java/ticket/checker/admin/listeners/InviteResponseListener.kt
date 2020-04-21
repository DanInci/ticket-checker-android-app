package ticket.checker.admin.listeners

import ticket.checker.beans.OrganizationInviteList

interface InviteResponseListener {
    fun inviteAccepted(inv: OrganizationInviteList)
    fun inviteDeclined(inv: OrganizationInviteList)
}