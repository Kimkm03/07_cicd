#!/bin/bash
# AWS CloudShell 에서 모니터링 EC2 에 들어간다 (.pem 없이, 학원 · 회사 망에서 22번이 막혔을 때)
#   EC2 Instance Connect 로 Bastion 과 모니터링 EC2 에 "60초짜리 임시 공개키" 를 넣고, Bastion 을 거쳐 SSH 한다
#
# 쓰는 법 (CloudShell, 리전 서울):
#   git clone https://github.com/<GitHub 아이디>/07_cicd.git
#   bash 07_cicd/load/cloudshell-ssh.sh <이름>                 # 들어가서 프롬프트가 ubuntu@ip-10-250-… 이면 성공
#   bash 07_cicd/load/cloudshell-ssh.sh <이름> 'docker ps'     # 명령 하나만 실행하고 나오기
#
# 필요한 것: Bastion 보안 그룹 22 가 CloudShell 에서 닿을 것 (스택 기본값 MyIpCidr = 0.0.0.0/0),
#           내 IAM 사용자에게 ec2-instance-connect:SendSSHPublicKey 권한
set -euo pipefail
NAME="${1:?사용법: bash cloudshell-ssh.sh <이름> [명령]}"; shift || true
export AWS_DEFAULT_REGION=ap-northeast-2

# 실행 중인 인스턴스의 ID 와 IP 를 이름표(Name)로 찾는다
q(){ aws ec2 describe-instances \
       --filters "Name=tag:Name,Values=$1" Name=instance-state-name,Values=running \
       --query "Reservations[0].Instances[0].$2" --output text; }
BASTION_ID=$(q "wonit-a-${NAME}-prd-bastion" InstanceId)
BASTION_IP=$(q "wonit-a-${NAME}-prd-bastion" PublicIpAddress)
MON_ID=$(q "wonit-a-${NAME}-prd-mon" InstanceId)
MON_IP=$(q "wonit-a-${NAME}-prd-mon" PrivateIpAddress)
[ "$BASTION_ID" = "None" ] && { echo "Bastion 을 못 찾았습니다 (이름 · 리전 확인)"; exit 1; }
[ "$MON_ID" = "None" ] && { echo "모니터링 EC2 를 못 찾았습니다"; exit 1; }

# CloudShell 홈에 임시 키를 한 번 만든다 (이 키는 CloudShell 안에만 있다)
KEY="$HOME/.ssh/wonit-cloudshell"
mkdir -p "$HOME/.ssh"; [ -f "$KEY" ] || ssh-keygen -t ed25519 -f "$KEY" -N '' -q

# 두 서버에 60초 동안 유효한 공개키를 넣는다 → 60초 안에 접속하면 된다
for id in "$BASTION_ID" "$MON_ID"; do
  aws ec2-instance-connect send-ssh-public-key --instance-id "$id" --instance-os-user ubuntu \
    --ssh-public-key "file://${KEY}.pub" --query Success --output text >/dev/null
done

O=(-i "$KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR)
if [ $# -eq 0 ]; then
  exec ssh -t "${O[@]}" -o ProxyCommand="ssh ${O[*]} -W %h:%p ubuntu@${BASTION_IP}" "ubuntu@${MON_IP}"
else
  exec ssh "${O[@]}" -o ProxyCommand="ssh ${O[*]} -W %h:%p ubuntu@${BASTION_IP}" "ubuntu@${MON_IP}" "$@"
fi
