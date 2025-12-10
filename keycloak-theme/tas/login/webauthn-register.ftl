<#import "template.ftl" as layout>
<#import "password-commons.ftl" as passwordCommons>

<@layout.registrationLayout displayInfo=false displayMessage=true displayRequiredFields=false; section>
    <#if section = "title">
        ${msg("webauthn-registration-title")}
    <#elseif section = "header">
        <!-- custom header handled in card -->
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
                    <div class="form-title">${msg("webauthn-registration-title")}</div>
                    <div class="hint">${msg("webauthn-registration-init-label")}</div>

                    <#if message?has_content>
                        <div class="${properties.kcAlertClass!} ${properties['kcAlert' + message.type?cap_first + 'Class']!}">
                            <span class="${properties.kcFeedbackTextClass!}">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <form id="register" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                        <div class="${properties.kcFormGroupClass!}">
                            <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
                            <input type="hidden" id="attestationObject" name="attestationObject"/>
                            <input type="hidden" id="publicKeyCredentialId" name="publicKeyCredentialId"/>
                            <input type="hidden" id="authenticatorLabel" name="authenticatorLabel"/>
                            <input type="hidden" id="transports" name="transports"/>
                            <input type="hidden" id="error" name="error"/>
                            <@passwordCommons.logoutOtherSessions/>
                        </div>
                    </form>

                    <div class="kc-form-group">
                        <button id="registerWebAuthn" type="button" class="btn" autofocus>
                            ${msg("doRegisterSecurityKey")}
                        </button>
                    </div>

                    <#if !isSetRetry?has_content && isAppInitiatedAction?has_content>
                        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-webauthn-settings-form" method="post">
                            <button type="submit" class="btn secondary" id="cancelWebAuthnAIA" name="cancel-aia" value="true">
                                ${msg("doCancel")}
                            </button>
                        </form>
                    </#if>
                </div>
            </div>
        </div>

        <script type="module">
            import { registerByWebAuthn } from "${url.resourcesPath}/js/webauthnRegister.js";
            const registerButton = document.getElementById('registerWebAuthn');
            registerButton?.addEventListener("click", function() {
                const input = {
                    challenge : '${challenge}',
                    userid : '${userid}',
                    username : '${username}',
                    signatureAlgorithms : [<#list signatureAlgorithms as sigAlg>${sigAlg?c}<#if sigAlg_has_next>,</#if></#list>],
                    rpEntityName : '${rpEntityName}',
                    rpId : '${rpId}',
                    attestationConveyancePreference : '${attestationConveyancePreference}',
                    authenticatorAttachment : '${authenticatorAttachment}',
                    requireResidentKey : '${requireResidentKey}',
                    userVerificationRequirement : '${userVerificationRequirement}',
                    createTimeout : ${createTimeout},
                    excludeCredentialIds : '${excludeCredentialIds}',
                    initLabel : "${msg("webauthn-registration-init-label")?no_esc}",
                    initLabelPrompt : "${msg("webauthn-registration-init-label-prompt")?no_esc}",
                    errmsg : "${msg("webauthn-unsupported-browser-text")?no_esc}"
                };
                registerByWebAuthn(input);
            });
        </script>
    </#if>
</@layout.registrationLayout>
