"use strict";(self.webpackChunkdynamic_asset_generator_wiki=self.webpackChunkdynamic_asset_generator_wiki||[]).push([[498],{3905:(e,t,r)=>{r.d(t,{Zo:()=>u,kt:()=>m});var n=r(7294);function o(e,t,r){return t in e?Object.defineProperty(e,t,{value:r,enumerable:!0,configurable:!0,writable:!0}):e[t]=r,e}function a(e,t){var r=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),r.push.apply(r,n)}return r}function i(e){for(var t=1;t<arguments.length;t++){var r=null!=arguments[t]?arguments[t]:{};t%2?a(Object(r),!0).forEach((function(t){o(e,t,r[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(r)):a(Object(r)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(r,t))}))}return e}function s(e,t){if(null==e)return{};var r,n,o=function(e,t){if(null==e)return{};var r,n,o={},a=Object.keys(e);for(n=0;n<a.length;n++)r=a[n],t.indexOf(r)>=0||(o[r]=e[r]);return o}(e,t);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);for(n=0;n<a.length;n++)r=a[n],t.indexOf(r)>=0||Object.prototype.propertyIsEnumerable.call(e,r)&&(o[r]=e[r])}return o}var l=n.createContext({}),c=function(e){var t=n.useContext(l),r=t;return e&&(r="function"==typeof e?e(t):i(i({},t),e)),r},u=function(e){var t=c(e.components);return n.createElement(l.Provider,{value:t},e.children)},d="mdxType",p={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},h=n.forwardRef((function(e,t){var r=e.components,o=e.mdxType,a=e.originalType,l=e.parentName,u=s(e,["components","mdxType","originalType","parentName"]),d=c(r),h=o,m=d["".concat(l,".").concat(h)]||d[h]||p[h]||a;return r?n.createElement(m,i(i({ref:t},u),{},{components:r})):n.createElement(m,i({ref:t},u))}));function m(e,t){var r=arguments,o=t&&t.mdxType;if("string"==typeof e||o){var a=r.length,i=new Array(a);i[0]=h;var s={};for(var l in t)hasOwnProperty.call(t,l)&&(s[l]=t[l]);s.originalType=e,s[d]="string"==typeof e?e:o,i[1]=s;for(var c=2;c<a;c++)i[c]=r[c];return n.createElement.apply(null,i)}return n.createElement.apply(null,r)}h.displayName="MDXCreateElement"},488:(e,t,r)=>{r.r(t),r.d(t,{assets:()=>l,contentTitle:()=>i,default:()=>p,frontMatter:()=>a,metadata:()=>s,toc:()=>c});var n=r(7462),o=(r(7294),r(3905));const a={},i="Shadowed Source",s={unversionedId:"json/texsources/shadowed",id:"json/texsources/shadowed",title:"Shadowed Source",description:"Source Type IDpalette_spread",source:"@site/docs/json/texsources/shadowed.md",sourceDirName:"json/texsources",slug:"/json/texsources/shadowed",permalink:"/DynamicAssetGenerator/json/texsources/shadowed",draft:!1,editUrl:"https://github.com/lukebemish/DynamicAssetGenerator/tree/docs/docs/json/texsources/shadowed.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Palette Spread Source",permalink:"/DynamicAssetGenerator/json/texsources/palette_spread"},next:{title:"Animation Splitter",permalink:"/DynamicAssetGenerator/json/texsources/splitter"}},l={},c=[],u={toc:c},d="wrapper";function p(e){let{components:t,...r}=e;return(0,o.kt)(d,(0,n.Z)({},u,r,{components:t,mdxType:"MDXLayout"}),(0,o.kt)("h1",{id:"shadowed-source"},"Shadowed Source"),(0,o.kt)("p",null,"Source Type ID: ",(0,o.kt)("inlineCode",{parentName:"p"},"dynamic_asset_generator:palette_spread")),(0,o.kt)("p",null,"Format:"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-json"},'{\n    "type": "dynamic_asset_generator:palette_spread",\n    "background": {   },\n    "foreground": {   },\n    "extend_palette_size": 6, // optional, defaults to 6\n    "highlight_strength": 72, // optional, defaults to 72\n    "shadow_strength": 72, // optional, defaults to 72\n    "uniformity": 1.0, // optional, defaults to 1.0\n}\n')),(0,o.kt)("p",null,"This source overlays the foreground texture on top of the background texture, and creates a directional shadow behind and around the foreground texture. Several other parameters can be configured:"),(0,o.kt)("ul",null,(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("inlineCode",{parentName:"li"},"extend_palette_size")," extends the extracted palette to this size by adding colors."),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("inlineCode",{parentName:"li"},"highlight_strength")," determines how much the highlight is emphasized. A higher number results in a brighter highlight."),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("inlineCode",{parentName:"li"},"shadow_strength")," determines how much the shadow is emphasized. A higher number results in a darker shadow."),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("inlineCode",{parentName:"li"},"uniformity")," determines how uniform the shadow and highlights are relative to the original background. A higher number results in a more uniform shadow.")))}p.isMDXComponent=!0}}]);