<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayWide=true>
    <#nested "header">
    <div class="page">
        <div class="card">
            <div class="brand">
                <div class="logo">🌀</div>
                <div class="brand-text">
                    <div class="title">TAS Portal</div>
                    <div class="subtitle">${realm.displayName!''}</div>
                </div>
            </div>

            <div class="form-area">
                <div class="form-title">
                    <#if login??>${msg("loginTitle")!}</#if>
                </div>
                <#nested "form">
                <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
                    <div class="hint">
                        ${msg("noAccount")} <a href="${url.registrationUrl}" class="link">${msg("doRegister")}</a>
                    </div>
                </#if>
            </div>
        </div>

        <div class="info">
            <div class="info-title">Welcome back</div>
            <div class="info-subtitle">Secure access to TAS with a clean, modern look.</div>
            <div class="info-list">
                <div>• Dark, focused login experience</div>
                <div>• Fast access to your applications</div>
                <div>• Protected by Keycloak</div>
            </div>
        </div>
    </div>
</@layout.registrationLayout>
