{{/*
Expand the name of the chart.
*/}}
{{- define "dbs-payment-order-integration-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "dbs-payment-order-integration-service.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "dbs-payment-order-integration-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "dbs-payment-order-integration-service.labels" -}}
helm.sh/chart: {{ include "dbs-payment-order-integration-service.chart" . }}
{{ include "dbs-payment-order-integration-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "dbs-payment-order-integration-service.selectorLabels" -}}
app.backbase.com/tier: dbs
app.kubernetes.io/instance: westerra-dbs-payment-order-integration-service
app.kubernetes.io/name: dbs-payment-order-integration-service
app.kubernetes.io/part-of: westerra
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "dbs-payment-order-integration-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "dbs-payment-order-integration-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
