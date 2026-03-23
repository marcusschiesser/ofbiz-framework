{{- define "order-lifecycle.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "order-lifecycle.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "order-lifecycle.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "order-lifecycle.labels" -}}
app.kubernetes.io/name: {{ include "order-lifecycle.name" . }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "order-lifecycle.selectorLabels" -}}
app.kubernetes.io/name: {{ include "order-lifecycle.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "order-lifecycle.frontendName" -}}
{{- printf "%s-frontend" (include "order-lifecycle.fullname" .) -}}
{{- end -}}

{{- define "order-lifecycle.backendName" -}}
{{- printf "%s-backend" (include "order-lifecycle.fullname" .) -}}
{{- end -}}

{{- define "order-lifecycle.frontendConfigMapName" -}}
{{- printf "%s-frontend-config" (include "order-lifecycle.fullname" .) -}}
{{- end -}}

{{- define "order-lifecycle.backendConfigMapName" -}}
{{- printf "%s-backend-config" (include "order-lifecycle.fullname" .) -}}
{{- end -}}

