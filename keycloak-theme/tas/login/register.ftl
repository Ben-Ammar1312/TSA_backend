<#import "template.ftl" as layout>

<@layout.registrationLayout displayInfo=false displayMessage=true displayRequiredFields=false; section>
    <#if section = "title">
        ${msg("registerTitle")}
    <#elseif section = "header">
        <!-- header handled in custom card -->
    <#elseif section = "form">
        <div class="page register-page">
            <div class="card wide">
                <div class="brand">
                    <div class="logo">🌀</div>
                    <div class="brand-text">
                        <div class="title">TAS Portal</div>
                        <div class="subtitle">${realm.displayName!''}</div>
                    </div>
                </div>

                <div class="form-area">
                    <div class="form-title">${msg("registerTitle")}</div>

                    <form id="kc-register-form" action="${url.registrationAction}" method="post">
                        <#if !realm.registrationEmailAsUsername?? || !realm.registrationEmailAsUsername>
                            <div class="${properties.kcFormGroupClass!} kc-form-group">
                                <label for="username" class="${properties.kcLabelClass!}">${msg("username")}</label>
                                <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${register.username!''}" type="text" autofocus autocomplete="username">
                                <#if messagesPerField.existsError('username')>
                                    <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get('username'))?no_esc}</span>
                                </#if>
                            </div>
                        </#if>

                        <div class="${properties.kcFormGroupClass!} kc-form-group">
                            <label for="email" class="${properties.kcLabelClass!}">${msg("email")}</label>
                            <input tabindex="2" id="email" class="${properties.kcInputClass!}" name="email" value="${register.email!''}" type="email" autocomplete="email">
                            <#if messagesPerField.existsError('email')>
                                <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get('email'))?no_esc}</span>
                            </#if>
                        </div>

                        <div class="two-col">
                            <div class="${properties.kcFormGroupClass!} kc-form-group">
                                <label for="firstName" class="${properties.kcLabelClass!}">${msg("firstName")}</label>
                                <input tabindex="3" id="firstName" class="${properties.kcInputClass!}" name="firstName" value="${register.firstName!''}" type="text" autocomplete="given-name">
                            </div>
                            <div class="${properties.kcFormGroupClass!} kc-form-group">
                                <label for="lastName" class="${properties.kcLabelClass!}">${msg("lastName")}</label>
                                <input tabindex="4" id="lastName" class="${properties.kcInputClass!}" name="lastName" value="${register.lastName!''}" type="text" autocomplete="family-name">
                            </div>
                        </div>

                        <div class="${properties.kcFormGroupClass!} kc-form-group">
                            <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
                            <input tabindex="5" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="new-password">
                            <#if messagesPerField.existsError('password')>
                                <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get('password'))?no_esc}</span>
                            </#if>
                        </div>

                        <div class="${properties.kcFormGroupClass!} kc-form-group">
                            <label for="password-confirm" class="${properties.kcLabelClass!}">${msg("passwordConfirm")}</label>
                            <input tabindex="6" id="password-confirm" class="${properties.kcInputClass!}" name="password-confirm" type="password" autocomplete="new-password">
                        </div>

                        <#if captchaRequired??>
                            <div class="${properties.kcFormGroupClass!} kc-form-group">
                                ${captcha}
                            </div>
                        </#if>

                        <div class="actions-row">
                            <a class="link" href="${url.loginUrl}">Back to Login</a>
                            <input class="btn" type="submit" value="${msg("doRegister")}">
                        </div>
                    </form>
                </div>
            </div>

            <div class="info">
                <div class="info-title">Welcome aboard</div>
                <div class="info-subtitle">Create your TAS account to access applications and manage your profile.</div>
                <div class="info-list">
                    <div>• Simple, focused signup</div>
                    <div>• Modern, secure experience</div>
                    <div>• Powered by Keycloak</div>
                </div>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
