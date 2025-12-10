<#import "template.ftl" as layout>

<@layout.registrationLayout displayInfo=false displayMessage=true displayRequiredFields=false; section>
    <#if section = "title">
        ${msg("webauthn-login-title")}
    <#elseif section = "header">
        <!-- header handled in custom card -->
    <#elseif section = "form">
        <div class="page login-page passkey-page">
            <div class="card auth-card">
                <div class="brand">
                    <div class="logo">🌀</div>
                    <div class="brand-text">
                        <div class="title">TAS Portal</div>
                        <div class="subtitle">${realm.displayName!''}</div>
                    </div>
                </div>

                <div class="form-area">
                    <div class="form-title">${msg("webauthn-login-title")}</div>

                    <#if message?has_content>
                        <div class="${properties.kcAlertClass!} ${properties['kcAlert' + message.type?cap_first + 'Class']!}">
                            <span class="${properties.kcFeedbackTextClass!}">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <form id="webauth" action="${url.loginAction}" method="post">
                        <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
                        <input type="hidden" id="authenticatorData" name="authenticatorData"/>
                        <input type="hidden" id="signature" name="signature"/>
                        <input type="hidden" id="credentialId" name="credentialId"/>
                        <input type="hidden" id="userHandle" name="userHandle"/>
                        <input type="hidden" id="error" name="error"/>
                    </form>

                    <#if authenticators??>
                        <form id="authn_select" class="${properties.kcFormClass!}">
                            <#list authenticators.authenticators as authenticator>
                                <input type="hidden" name="authn_use_chk" value="${authenticator.credentialId}"/>
                            </#list>
                        </form>
                    </#if>

                    <#if shouldDisplayAuthenticators?? && shouldDisplayAuthenticators && authenticators?? && authenticators.authenticators?size gt 0>
                        <div class="auth-option-list">
                            <#list authenticators.authenticators as authenticator>
                                <div class="auth-option">
                                    <div class="auth-option-icon">
                                        <i class="${(properties['${authenticator.transports.iconClass}'])!'${properties.kcWebAuthnDefaultIcon!}'} ${properties.kcSelectAuthListItemIconPropertyClass!}"></i>
                                    </div>
                                    <div class="auth-option-text">
                                        <div class="auth-option-title">
                                            ${kcSanitize(msg('${authenticator.label}'))?no_esc}
                                        </div>
                                        <#if authenticator.transports?? && authenticator.transports.displayNameProperties?has_content>
                                            <div class="auth-option-desc">
                                                <#list authenticator.transports.displayNameProperties as nameProperty>
                                                    <span>${kcSanitize(msg('${nameProperty!}'))?no_esc}</span><#if nameProperty?has_next><span>, </span></#if>
                                                </#list>
                                            </div>
                                        </#if>
                                        <div class="auth-option-desc">
                                            <span>${kcSanitize(msg('webauthn-createdAt-label'))?no_esc}</span>
                                            <span>${kcSanitize(authenticator.createdAt)?no_esc}</span>
                                        </div>
                                    </div>
                                </div>
                            </#list>
                        </div>
                    </#if>

                    <div class="kc-form-group">
                        <button id="authenticateWebAuthnButton" type="button" autofocus="autofocus" class="btn">
                            ${kcSanitize(msg("webauthn-doAuthenticate"))}
                        </button>
                    </div>

                    <#if realm.registrationAllowed && !registrationDisabled??>
                        <div class="hint">
                            ${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}" class="link">${msg("doRegister")}</a>
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

        <script type="module">
            import { authenticateByWebAuthn } from "${url.resourcesPath}/js/webauthnAuthenticate.js";
            const authButton = document.getElementById('authenticateWebAuthnButton');
            authButton.addEventListener("click", function() {
                const input = {
                    isUserIdentified : ${isUserIdentified},
                    challenge : '${challenge}',
                    userVerification : '${userVerification}',
                    rpId : '${rpId}',
                    createTimeout : ${createTimeout},
                    errmsg : "${msg("webauthn-unsupported-browser-text")?no_esc}"
                };
                authenticateByWebAuthn(input);
            });
        </script>
    </#if>
</@layout.registrationLayout>
