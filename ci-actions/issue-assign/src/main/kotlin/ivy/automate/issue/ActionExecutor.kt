package ivy.automate.issue

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import ivy.automate.base.Constants
import ivy.automate.base.github.GitHubIssueArgs
import ivy.automate.base.github.GitHubService
import ivy.automate.base.github.model.GitHubUser
import ivy.automate.base.github.model.NotBlankTrimmedString

suspend fun Action.AlreadyTaken.execute(
    gitHub: GitHubService,
    args: GitHubIssueArgs
): Either<String, String> = either {
    val commentText = buildString {
        warn(user)
        append(" this issue is already taken by @${assignee.username.value}.")
        append("\n**Do not start working on it!**")
        append("\nPlease, [pick another one](${Constants.ISSUES_URL}).")
        readContributingMd()
    }
    comment(gitHub, args, commentText)
    commentText
}

suspend fun Action.NotApproved.execute(
    gitHub: GitHubService,
    args: GitHubIssueArgs
): Either<String, String> = either {
    val commentText = buildString {
        warn(user)
        append(" this issue is **not approved**, yet.")
        append("\n@${Constants.IVY_ADMIN} must approve it first.")
        readContributingMd()
    }
    comment(gitHub, args, commentText)
    commentText
}

suspend fun Action.AssignIssue.execute(
    gitHub: GitHubService,
    args: GitHubIssueArgs
): Either<String, String> = either {
    gitHub.assignIssue(
        pat = args.pat,
        issueNumber = issueNumber,
        assignee = user.username
    ).mapLeft {
        "Failed to assign issue: $it"
    }.bind()

    val commentText = buildString {
        append("Thank you for your interest @${user.username.value}! \uD83C\uDF89")
        append("\nIssue #${issueNumber.value} is assigned to you. You can work on it! ✅")
        append("\n\n_If you don't want to work on it now, please un-assign yourself")
        append(" so other contributors can take it._")
        readContributingMd()
    }
    comment(gitHub, args, commentText)
    commentText
}

private suspend fun Raise<String>.comment(
    gitHub: GitHubService,
    args: GitHubIssueArgs,
    text: String
) {
    gitHub.commentIssue(
        pat = args.pat,
        issueNumber = args.issueNumber,
        text = NotBlankTrimmedString(text)
    ).mapLeft {
        "Failed to comment: $it"
    }.bind()
}

private fun StringBuilder.warn(user: GitHubUser) {
    append("⚠️ Hey @${user.username.value},")
}

private fun StringBuilder.readContributingMd() {
    append("\n\n")
    append("Also, make sure to read our [Contribution Guidelines](${Constants.CONTRIBUTING_URL}).")
}
