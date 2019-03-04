import groovy.json.JsonBuilder

utils = load 'ci/utils.groovy'

/**
 * Methods for interacting with GitHub API and related tools.
 **/

/* Comments -------------------------------------------------------*/

def notify(message) {
  def githubIssuesUrl = 'https://api.github.com/repos/status-im/status-react/issues'
  def changeId = changeId() 
  if (changeId == null) { return }
  def msgObj = [body: message]
  def msgJson = new JsonBuilder(msgObj).toPrettyString()
  withCredentials([usernamePassword(
    credentialsId:  'status-im-auto',
    usernameVariable: 'GH_USER',
    passwordVariable: 'GH_PASS'
  )]) {
    sh """
      curl --silent \
        -u '${GH_USER}:${GH_PASS}' \
        --data '${msgJson}' \
        -H "Content-Type: application/json" \
        "${githubIssuesUrl}/${changeId}/comments"
    """.trim()
  }
}

def notifyFull(urls) {
  def msg = "#### :white_check_mark: "
  msg += "[${env.JOB_NAME}${currentBuild.displayName}](${currentBuild.absoluteUrl}) "
  msg += "CI BUILD SUCCESSFUL in ${utils.buildDuration()} (${GIT_COMMIT.take(8)})\n"
  msg += '| | | | | |\n'
  msg += '|-|-|-|-|-|\n'
  msg += "| [Android](${urls.Apk}) ([e2e](${urls.Apke2e})) "
  msg += "| [iOS](${urls.iOS}) ([e2e](${urls.iOSe2e})) |"
  if (urls.Mac != null) {
    msg += " [MacOS](${urls.Mac}) | [AppImage](${urls.App}) | [Windows](${urls.Win}) |"
  } else {
    msg += " ~~MacOS~~ | ~~AppImage~~ | ~~Windows~~~ |"
  }
  notify(msg)
}

def notifyPRFailure() {
  def d = ":small_orange_diamond:"
  def msg = "#### :x: "
  msg += "[${env.JOB_NAME}${currentBuild.displayName}](${currentBuild.absoluteUrl}) ${d} "
  msg += "${utils.buildDuration()} ${d} ${GIT_COMMIT.take(8)} ${d} "
  msg += "[:page_facing_up: build log](${currentBuild.absoluteUrl}/consoleText)"
  //msg += "Failed in stage: ${env.STAGE_NAME}\n"
  //msg += "```${currentBuild.rawBuild.getLog(5)}```"
  notify(msg)
}

def notifyPRSuccess() {
  def d = ":small_blue_diamond:"
  def msg = "#### :heavy_check_mark: "
  def type = utils.getBuildType() == 'e2e' ? ' e2e' : ''
  msg += "[${env.JOB_NAME}${currentBuild.displayName}](${currentBuild.absoluteUrl}) ${d} "
  msg += "${utils.buildDuration()} ${d} ${GIT_COMMIT.take(8)} ${d} "
  msg += "[:package: ${env.BUILD_PLATFORM}${type} package](${env.PKG_URL})"
  notify(msg)
}

/* Releases -------------------------------------------------------*/

def getPrevRelease() {
  return sh(returnStdout: true,
    script: "git branch --format '%(refname)' '--sort 'creatordate' --list 'release/*'"
  ).trim().split("\\r?\\n").last()
}

def getDiffUrl(prev, current) {
  return "https://github.com/status-im/status-react/compare/${prev}...${current}"
}

def getReleaseChanges() {
  def prevRelease = getPrevRelease()
  def curRelease = utils.branchName()
  try {
    def changes = sh(returnStdout: true,
      script: """
        git log \
          --cherry-pick \
          --right-only \
          --no-merges \
          --format='* %h %s' \
          ${prevRelease}..HEAD
      """
    ).trim()
  } catch (Exception ex) {
    println 'ERROR: Failed to retrieve changes.'
    println ex.message
    return ':warning: __Please fill me in!__'
  }
  return changes + '\nDiff:' + getDiffUrl()
}

def releaseDelete(Map args) {
  sh """
    github-release upload \
      --replace \
      -u '${args.user}' \
      -r '${args.repo}' \
      -t '${args.version}' \
  """
}

def releaseUpload(Map args) {
  sh """
    github-release upload \
      -u '${args.user}' \
      -r '${args.repo}' \
      -t '${args.version}' \
      -f '${args.files}
  """
}

def releasePublish(Map args) {
  sh """
    github-release release \
      -u '${args.user}' \
      -r '${args.repo}' \
      -n '${args.version}' \
      -t '${args.version}' \
      -c '${args.branch}' \
      -d '${args.desc}' \
      ${args.draft ? "--draft" : ""}
  """
}

def publishRelease(Map args) {
  def config = [
    draft: true,
    user: 'status-im',
    repo: 'status-react',
    version: args.version,
    branch: utils.branchName(),
    desc: getReleaseChanges(),
    files: "pkg/${args.regex}"
  ]
  /* we release only for mobile right now */
  withCredentials([usernamePassword(
    credentialsId:  'status-im-auto',
    usernameVariable: 'GITHUB_USER',
    passwordVariable: 'GITHUB_TOKEN'
  )]) {
    releaseDelete(config) /* so we can re-release it */
    releasePublish(config)
    releaseUpload(config)
  }
}

def publishReleaseMobile() {
  publishRelease(
    version: utils.getVersion('mobile_files')+'-mobile',
    regex: '*release.{ipa,apk,sha256}'
  )
}

return this
