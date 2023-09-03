"use strict";(self.webpackChunkdynamic_asset_generator_wiki=self.webpackChunkdynamic_asset_generator_wiki||[]).push([[789],{3905:(e,t,r)=>{r.d(t,{Zo:()=>c,kt:()=>m});var n=r(7294);function a(e,t,r){return t in e?Object.defineProperty(e,t,{value:r,enumerable:!0,configurable:!0,writable:!0}):e[t]=r,e}function o(e,t){var r=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),r.push.apply(r,n)}return r}function i(e){for(var t=1;t<arguments.length;t++){var r=null!=arguments[t]?arguments[t]:{};t%2?o(Object(r),!0).forEach((function(t){a(e,t,r[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(r)):o(Object(r)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(r,t))}))}return e}function l(e,t){if(null==e)return{};var r,n,a=function(e,t){if(null==e)return{};var r,n,a={},o=Object.keys(e);for(n=0;n<o.length;n++)r=o[n],t.indexOf(r)>=0||(a[r]=e[r]);return a}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(n=0;n<o.length;n++)r=o[n],t.indexOf(r)>=0||Object.prototype.propertyIsEnumerable.call(e,r)&&(a[r]=e[r])}return a}var s=n.createContext({}),u=function(e){var t=n.useContext(s),r=t;return e&&(r="function"==typeof e?e(t):i(i({},t),e)),r},c=function(e){var t=u(e.components);return n.createElement(s.Provider,{value:t},e.children)},p="mdxType",d={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},f=n.forwardRef((function(e,t){var r=e.components,a=e.mdxType,o=e.originalType,s=e.parentName,c=l(e,["components","mdxType","originalType","parentName"]),p=u(r),f=a,m=p["".concat(s,".").concat(f)]||p[f]||d[f]||o;return r?n.createElement(m,i(i({ref:t},c),{},{components:r})):n.createElement(m,i({ref:t},c))}));function m(e,t){var r=arguments,a=t&&t.mdxType;if("string"==typeof e||a){var o=r.length,i=new Array(o);i[0]=f;var l={};for(var s in t)hasOwnProperty.call(t,s)&&(l[s]=t[s]);l.originalType=e,l[p]="string"==typeof e?e:a,i[1]=l;for(var u=2;u<o;u++)i[u]=r[u];return n.createElement.apply(null,i)}return n.createElement.apply(null,r)}f.displayName="MDXCreateElement"},5634:(e,t,r)=>{r.r(t),r.d(t,{assets:()=>s,contentTitle:()=>i,default:()=>d,frontMatter:()=>o,metadata:()=>l,toc:()=>u});var n=r(7462),a=(r(7294),r(3905));const o={},i="Foreground Transfer Source",l={unversionedId:"json/texsources/foreground_transfer",id:"json/texsources/foreground_transfer",title:"Foreground Transfer Source",description:"Source Type IDforeground_transfer",source:"@site/docs/json/texsources/foreground_transfer.md",sourceDirName:"json/texsources",slug:"/json/texsources/foreground_transfer",permalink:"/DynamicAssetGenerator/json/texsources/foreground_transfer",draft:!1,editUrl:"https://github.com/lukebemishprojects/DynamicAssetGenerator/tree/docs/docs/json/texsources/foreground_transfer.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Texture File Source",permalink:"/DynamicAssetGenerator/json/texsources/file"},next:{title:"Mask Source",permalink:"/DynamicAssetGenerator/json/texsources/mask"}},s={},u=[],c={toc:u},p="wrapper";function d(e){let{components:t,...r}=e;return(0,a.kt)(p,(0,n.Z)({},c,r,{components:t,mdxType:"MDXLayout"}),(0,a.kt)("h1",{id:"foreground-transfer-source"},"Foreground Transfer Source"),(0,a.kt)("p",null,"Source Type ID: ",(0,a.kt)("inlineCode",{parentName:"p"},"dynamic_asset_generator:foreground_transfer")),(0,a.kt)("p",null,"Format:"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-json"},'{\n    "type": "dynamic_asset_generator:foreground_transfer",\n    "background": {   },\n    "full": {   },\n    "new_background": {   },\n    "trim_trailing": true, // optional, default true\n    "force_neighbors": true, // optional, default true\n    "fill_holes": true, // optional, default true\n    "extend_palette_size": 0, // optional, default 6\n    "close_cutoff": 2 // optional, default 2\n}\n')),(0,a.kt)("p",null,(0,a.kt)("inlineCode",{parentName:"p"},"background"),", ",(0,a.kt)("inlineCode",{parentName:"p"},"full"),", and ",(0,a.kt)("inlineCode",{parentName:"p"},"new_background")," are texture sources. The Foreground Transfer Source is similar to the Combined Paletted Image Source. First, an overlay image and image storing palette changes are extracted from the ",(0,a.kt)("inlineCode",{parentName:"p"},"background")," and ",(0,a.kt)("inlineCode",{parentName:"p"},"full"),". Then, this same set of palette changes and overlay is applied to the ",(0,a.kt)("inlineCode",{parentName:"p"},"new_background")," image. Several other parameters can be configured:"),(0,a.kt)("ul",null,(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("inlineCode",{parentName:"li"},"extend_palette_size")," extends the extracted palette to this size by adding colors."),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("inlineCode",{parentName:"li"},"trim_trailing")," removes transparent overlay pixels or palette change pixels not connected to a solid overlay pixel. Defaults to ",(0,a.kt)("inlineCode",{parentName:"li"},"false"),"."),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("inlineCode",{parentName:"li"},"force_neighbors")," records the palette state for any pixel next to a solid overlay pixel, regardless of whether it changes between the two textures. Defaults to ",(0,a.kt)("inlineCode",{parentName:"li"},"false"),"."),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("inlineCode",{parentName:"li"},"fill_holes")," attempts to fill holes in the texture, possibly with some success. Defaults to ",(0,a.kt)("inlineCode",{parentName:"li"},"false"),"."),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("inlineCode",{parentName:"li"},"close_cutoff")," determines where the line is drawn between pixels not in the palette and pixels that are formed by a semi-transparent overlay on a palette color. Increasing this value makes it more lenient. Defaults to ",(0,a.kt)("inlineCode",{parentName:"li"},"2"),", which is usually a good value.")))}d.isMDXComponent=!0}}]);