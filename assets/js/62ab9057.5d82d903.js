"use strict";(self.webpackChunkdynamic_asset_generator_wiki=self.webpackChunkdynamic_asset_generator_wiki||[]).push([[673],{3905:(e,t,r)=>{r.d(t,{Zo:()=>l,kt:()=>y});var n=r(7294);function a(e,t,r){return t in e?Object.defineProperty(e,t,{value:r,enumerable:!0,configurable:!0,writable:!0}):e[t]=r,e}function o(e,t){var r=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),r.push.apply(r,n)}return r}function s(e){for(var t=1;t<arguments.length;t++){var r=null!=arguments[t]?arguments[t]:{};t%2?o(Object(r),!0).forEach((function(t){a(e,t,r[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(r)):o(Object(r)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(r,t))}))}return e}function i(e,t){if(null==e)return{};var r,n,a=function(e,t){if(null==e)return{};var r,n,a={},o=Object.keys(e);for(n=0;n<o.length;n++)r=o[n],t.indexOf(r)>=0||(a[r]=e[r]);return a}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(n=0;n<o.length;n++)r=o[n],t.indexOf(r)>=0||Object.prototype.propertyIsEnumerable.call(e,r)&&(a[r]=e[r])}return a}var c=n.createContext({}),u=function(e){var t=n.useContext(c),r=t;return e&&(r="function"==typeof e?e(t):s(s({},t),e)),r},l=function(e){var t=u(e.components);return n.createElement(c.Provider,{value:t},e.children)},p="mdxType",m={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},d=n.forwardRef((function(e,t){var r=e.components,a=e.mdxType,o=e.originalType,c=e.parentName,l=i(e,["components","mdxType","originalType","parentName"]),p=u(r),d=a,y=p["".concat(c,".").concat(d)]||p[d]||m[d]||o;return r?n.createElement(y,s(s({ref:t},l),{},{components:r})):n.createElement(y,s({ref:t},l))}));function y(e,t){var r=arguments,a=t&&t.mdxType;if("string"==typeof e||a){var o=r.length,s=new Array(o);s[0]=d;var i={};for(var c in t)hasOwnProperty.call(t,c)&&(i[c]=t[c]);i.originalType=e,i[p]="string"==typeof e?e:a,s[1]=i;for(var u=2;u<o;u++)s[u]=r[u];return n.createElement.apply(null,s)}return n.createElement.apply(null,r)}d.displayName="MDXCreateElement"},1573:(e,t,r)=>{r.r(t),r.d(t,{assets:()=>c,contentTitle:()=>s,default:()=>m,frontMatter:()=>o,metadata:()=>i,toc:()=>u});var n=r(7462),a=(r(7294),r(3905));const o={},s="Mask Sources",i={unversionedId:"json/texsources/masks/category",id:"json/texsources/masks/category",title:"Mask Sources",description:"Source Type IDmask",source:"@site/docs/json/texsources/masks/category.md",sourceDirName:"json/texsources/masks",slug:"/json/texsources/masks/category",permalink:"/DynamicAssetGenerator/json/texsources/masks/category",draft:!1,editUrl:"https://github.com/lukebemish/DynamicAssetGenerator/tree/docs/docs/json/texsources/masks/category.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Texture Generation",permalink:"/DynamicAssetGenerator/json/texsources/category"},next:{title:"Add Mask Source",permalink:"/DynamicAssetGenerator/json/texsources/masks/add"}},c={},u=[],l={toc:u},p="wrapper";function m(e){let{components:t,...r}=e;return(0,a.kt)(p,(0,n.Z)({},l,r,{components:t,mdxType:"MDXLayout"}),(0,a.kt)("h1",{id:"mask-sources"},"Mask Sources"),(0,a.kt)("p",null,"Source Type ID: ",(0,a.kt)("inlineCode",{parentName:"p"},"dynamic_asset_generator:mask")),(0,a.kt)("p",null,"Format:"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-json"},'{\n    "type": "dynamic_asset_generator:mask",\n    "mask": {   },\n    "input": {   }\n}\n')),(0,a.kt)("ul",null,(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("inlineCode",{parentName:"li"},"mask")," is a texture source used for the mask texture."),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("inlineCode",{parentName:"li"},"input")," is a texture source used for the input texture.")),(0,a.kt)("p",null,"The output texture will be identical to the input, with its alpha multiplied by that of the mask texture. Textures are scaled to fit the wider texture if necessary."),(0,a.kt)("h1",{id:"generating-masks"},"Generating Masks"),(0,a.kt)("p",null,"DynamicAssetGenerator has a number of texture sources meant for generating masks. These operations are all in the ",(0,a.kt)("inlineCode",{parentName:"p"},"dynamic_asset_generator:mask/")," folder. These sources all do a given operation in the alpha channel, and make no guarantees about the content of other channels."))}m.isMDXComponent=!0}}]);