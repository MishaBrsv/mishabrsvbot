#!/bin/bash

branch=$(git ls-remote --heads origin | grep $(git rev-parse HEAD) | cut -d / -f 3)
echo "info: The branch name is ${branch}"

commitm=$(git log --format=%B -n 1)
echo "info: The commit message is ${commitm}"

author=$(git --no-pager show -s --format='%ae')
echo "info: The commit author is ${author}"

sendMessage(){
  curl -F chat_id="-1001359397780" \
     -F parse_mode="Markdown" \
     -F disable_web_page_preview="true" \
     -F text="Project is *Золотой кальмар*
Branch: ${branch}
Author: ${author}
Message: ${commitm}
Status: ${status}
${CI_PROJECT_URL}/-/jobs
     " \
     https://api.telegram.org:443/bot1207800128:AAHimMcrTYV1PbzdTAvmQaawoSmlW4tQEfw/sendMessage
}

if [ "$1" = "ok" ]; then
  status=👍
  sendMessage
elif [ "$1" = "error" ]; then
  status=😱
  sendMessage
  exit 1
fi
