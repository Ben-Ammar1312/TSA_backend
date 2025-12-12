<#import "template.ftl" as layout>

<@layout.registrationLayout displayInfo=false displayMessage=true displayRequiredFields=false; section>
    <#if section = "title">
        ${msg("loginTitle", (realm.displayName!''))}
    <#elseif section = "header">
        <!-- header handled in custom card -->
    <#elseif section = "form">
        <div class="page login-page">
            <div class="card">
                <div class="brand">
                    <div class="logo">🌀</div>
                    <div class="brand-text">
                        <div class="title">TAS Portal</div>
                        <div class="subtitle">${realm.displayName!''}</div>
                    </div>
                </div>

                <div class="form-area">
                    <div class="form-title">Sign in to TAS</div>

                    <#if message?has_content>
                        <div class="${properties.kcAlertClass!} ${properties['kcAlert' + message.type?cap_first + 'Class']!}">
                            <span class="${properties.kcFeedbackTextClass!}">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <#if realm.password>
                        <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                            <input type="hidden" id="id-hidden-input" name="credentialId"<#if auth?has_content && auth.selectedCredential?has_content> value="${auth.selectedCredential}"</#if>>

                            <div class="${properties.kcFormGroupClass!} kc-form-group">
                                <label for="username" class="${properties.kcLabelClass!}">${msg("username")}</label>
                                <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${login.username!''}" type="text" autofocus autocomplete="username">
                            </div>

                            <div class="${properties.kcFormGroupClass!} kc-form-group">
                                <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
                                <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="current-password">
                            </div>

                            <div class="${properties.kcFormGroupClass!} kc-form-group">
                                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                                    <div class="${properties.kcFormOptionsWrapperClass!}">
                                        <#if realm.rememberMe && !usernameEditDisabled??>
                                            <label class="${properties.kcLabelClass!}">
                                                <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" <#if login.rememberMe??>checked</#if>> ${msg("rememberMe")}
                                            </label>
                                        </#if>
                                        <#if realm.resetPasswordAllowed>
                                            <a tabindex="5" class="link" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a>
                                        </#if>
                                    </div>
                                </div>
                            </div>

                            <div id="kc-form-buttons" class="${properties.kcFormGroupClass!} kc-form-buttons">
                                <input tabindex="4" class="btn" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}">
                            </div>
                        </form>
                    </#if>

                    <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
                        <div class="hint">
                            ${msg("noAccount")} <a href="${url.registrationUrl}" class="link">${msg("doRegister")}</a>
                        </div>
                    </#if>

                    <#if auth?has_content && auth.showTryAnotherWayLink()>
                        <form class="try-another-form" action="${url.loginAction}" method="post">
                            <input type="hidden" name="tryAnotherWay" value="on">
                            <button type="submit" class="link try-another-link">
                                ${msg("doTryAnotherWay")}
                            </button>
                        </form>
                    </#if>

                </div>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
