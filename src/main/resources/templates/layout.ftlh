<#--
  Building blocks shared by every page.

  A page imports this file and wraps its content in the page macro:

    <#import "layout.ftlh" as layout>
    <@layout.page title=layout.msg("landing.title")>
      <main>...</main>
    </@layout.page>

  Templates use the .ftlh extension, so FreeMarker escapes everything printed with ${...} as HTML.
-->

<#--
  Returns a text from messages.properties.
  Optional args fill the {0}, {1}, ... placeholders: msg("checkpoint.numbered", [3]) gives "Checkpoint 3".
-->
<#function msg code args=[]>
  <#return springMacroRequestContext.getMessage(code, args)>
</#function>

<#--
  Returns the message for the first problem with a field of the check-in form, or "" if the field is fine.
  The problems come from Spring's validation of CheckInForm (see CheckInController.checkIn).
-->
<#function fieldError field>
  <#if !(springMacroRequestContext.getErrors("form"))??>
    <#return "">
  </#if>
  <#local errors = springMacroRequestContext.getErrors("form")>
  <#if !errors.hasFieldErrors(field)>
    <#return "">
  </#if>
  <#return springMacroRequestContext.getMessage(errors.getFieldError(field))>
</#function>

<#--
  How a checkpoint is called on the pages: "Finish", its name, or "Checkpoint 3" when it has no name.
-->
<#function checkpointLabel checkpoint>
  <#if checkpoint.finish>
    <#return msg("checkpoint.finish")>
  <#elseif checkpoint.name?has_content>
    <#return checkpoint.name>
  <#elseif checkpoint.number??>
    <#return msg("checkpoint.numbered", [checkpoint.number])>
  <#else>
    <#return msg("checkpoint.unnamed")>
  </#if>
</#function>

<#--
  The page shell: <head>, the header with the QR Timing logo, and the footer.

  title       the browser tab title
  width       content width: "landing" (1120px), "wide" (880px, check-in) or "narrow" (520px, success and errors)
  link, linkText
              optional link on the right of the header
  footer      footer text
  description meta description for search engines; only the landing page is meant to be found
-->
<#macro page title width="narrow" link="" linkText="" footer=msg("footer.service") description="">
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${title}</title>
  <#if description?has_content>
  <meta name="description" content="${description}">
  <#else>
  <#-- Check-in pages carry the secret QR key in their address; keep them out of search engines. -->
  <meta name="robots" content="noindex, nofollow">
  </#if>
  <meta name="theme-color" content="#15803D">
  <link rel="icon" href="/static/img/favicon.svg" type="image/svg+xml">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@500;700&family=Manrope:wght@600;700;800&display=swap">
  <link rel="stylesheet" href="/static/css/qr-timing.css">
</head>
<body>
<div class="page">
  <header class="site-header container container--${width}">
    <div class="brand">
      <@logo/>
      <span class="brand__name">${msg("app.name")}</span>
    </div>
    <#if link?has_content>
    <a class="site-header__link" href="${link}">${linkText}</a>
    </#if>
  </header>

  <#nested>

  <footer class="site-footer site-footer--${width} container container--${width}">${footer}</footer>
</div>
</body>
</html>
</#macro>

<#-- The QR Timing logo: a stylised QR code on a green tile. -->
<#macro logo>
  <svg class="brand__logo" width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">
    <rect width="32" height="32" rx="9" fill="#15803D"/>
    <rect x="6" y="6" width="8" height="8" rx="1.5" fill="none" stroke="#FFFFFF" stroke-width="2"/>
    <rect x="18" y="6" width="8" height="8" rx="1.5" fill="none" stroke="#FFFFFF" stroke-width="2"/>
    <rect x="6" y="18" width="8" height="8" rx="1.5" fill="none" stroke="#FFFFFF" stroke-width="2"/>
    <rect x="18" y="18" width="3.5" height="3.5" fill="#FFFFFF"/>
    <rect x="23" y="23" width="3.5" height="3.5" fill="#FFFFFF"/>
    <rect x="23" y="18" width="3.5" height="3.5" fill="#FFFFFF"/>
  </svg>
</#macro>
