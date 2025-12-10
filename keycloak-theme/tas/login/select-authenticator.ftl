<#import "template.ftl" as layout>

<@layout.registrationLayout displayInfo=false displayMessage=true displayRequiredFields=false; section>
    <#if section = "title">
        ${msg("loginChooseAuthenticator")}
    <#elseif section = "header">
        <!-- header handled in custom card -->
    <#elseif section = "form">
        <div class="page login-page auth-choice">
            <div class="card auth-card">
                <div class="brand">
                    <div class="logo">🌀</div>
                    <div class="brand-text">
                        <div class="title">TAS Portal</div>
                        <div class="subtitle">${realm.displayName!''}</div>
                    </div>
                </div>

                <div class="form-area">
                    <div class="form-title">${msg("loginChooseAuthenticator")}</div>
                    <div class="hint">Choose how you'd like to continue.</div>

                    <form id="kc-select-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                        <div class="auth-option-list">
                            <#list auth.authenticationSelections as authenticationSelection>
                                <button class="auth-option" type="submit" name="authenticationExecution" value="${authenticationSelection.authExecId}">
                                    <div class="auth-option-icon">
                                        <i class="${properties['${authenticationSelection.iconCssClass}']!authenticationSelection.iconCssClass} ${properties.kcSelectAuthListItemIconPropertyClass!}"></i>
                                    </div>
                                    <div class="auth-option-text">
                                        <div class="auth-option-title">
                                            ${msg('${authenticationSelection.displayName}')}
                                        </div>
                                        <div class="auth-option-desc">
                                            ${msg('${authenticationSelection.helpText}')}
                                        </div>
                                    </div>
                                    <div class="auth-option-arrow">
                                        <i class="${properties.kcSelectAuthListItemArrowIconClass!}"></i>
                                    </div>
                                </button>
                            </#list>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
