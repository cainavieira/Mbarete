#!/bin/sh
# Convida uma pessoa (UC-13): roda o modo convidar da api num pod que se apaga
# no fim. Na VM, da pasta do repositório:
#   sudo producao/convidar.sh fulano@exemplo.com
set -eu

if [ $# -ne 1 ]; then
  echo "Uso: sudo producao/convidar.sh e-mail" >&2
  exit 1
fi

# A versão da api no ar: o convite grava no esquema que ela conhece
imagem=$(kubectl -n mbarete get deploy api \
  -o jsonpath='{.spec.template.spec.containers[0].image}')

# O banco e os limites do initContainer de migração, no api.yaml
kubectl -n mbarete run convite --rm -i --restart=Never --image="$imagem" \
  --overrides="$(cat <<EOF
{
  "spec": {
    "securityContext": {
      "runAsNonRoot": true,
      "seccompProfile": {"type": "RuntimeDefault"}
    },
    "containers": [{
      "name": "convite",
      "image": "$imagem",
      "args": ["convidar", "$1"],
      "env": [
        {"name": "MBARETE_DB_URL", "value": "jdbc:postgresql://banco-rw:5432/mbarete"},
        {"name": "MBARETE_DB_USUARIO", "valueFrom": {"secretKeyRef": {"name": "banco-app", "key": "username"}}},
        {"name": "MBARETE_DB_SENHA", "valueFrom": {"secretKeyRef": {"name": "banco-app", "key": "password"}}}
      ],
      "resources": {"limits": {"memory": "768Mi"}},
      "securityContext": {
        "allowPrivilegeEscalation": false,
        "capabilities": {"drop": ["ALL"]}
      }
    }]
  }
}
EOF
)"
