import{r}from"./vue.esm-bundler-Dh6gGtoH.js";import{A as g,B as C,F as B,M as v,a as S,S as y}from"./MoneyAmount--1yKshzQ.js";import"./index-YxUb3WpO.js";import"./_commonjsHelpers-Cpj98o6Y.js";const M=[{id:"1",label:"总成 A-001",qty:1,children:[{id:"2",label:"零件 B-002",qty:2,cost:120},{id:"3",label:"零件 C-003",qty:1,cost:80}]}],A=[{id:1,title:"业务员",assignee:"张三",status:"APPROVED",time:"06-15 09:00"},{id:2,title:"销售主管",assignee:"李四",status:"PENDING"},{id:3,title:"总经理",assignee:"王总",status:"PENDING"}],R={title:"ERP/Custom"},a={render:()=>({components:{FigureNumberSearch:B},setup(){return{v:r("")}},template:'<FigureNumberSearch v-model="v" />'})},o={render:()=>({components:{FigureNumberSearch:B},template:'<FigureNumberSearch model-value="DWG-001" disabled />'})},t={render:()=>({components:{BomTree:C},setup(){return{sampleBom:M}},template:'<BomTree :data="sampleBom" />'})},s={render:()=>({components:{BomTree:C},setup(){return{sampleBom:M}},template:'<BomTree :data="sampleBom" show-cost editable />'})},n={render:()=>({components:{ScanTrigger:y},setup(){return{code:r("")}},template:'<ScanTrigger v-model="code" />'})},c={render:()=>({components:{MachineLoadBar:v},template:'<MachineLoadBar :percent="55" machine-name="CNC-01" />'})},m={render:()=>({components:{MachineLoadBar:v},template:'<MachineLoadBar :percent="78" machine-name="CNC-02" />'})},p={render:()=>({components:{MachineLoadBar:v},template:'<MachineLoadBar :percent="95" machine-name="CNC-03" />'})},d={render:()=>({components:{ApprovalChainRenderer:g},setup(){return{sampleChain:A}},template:'<ApprovalChainRenderer :nodes="sampleChain" />'})},i={render:()=>({components:{ApprovalChainRenderer:g},setup(){return{nodes:A.map(h=>({...h,status:"APPROVED"}))}},template:'<ApprovalChainRenderer :nodes="nodes" />'})},u={render:()=>({components:{MoneyAmount:S},setup(){return{v:r(1e3)}},template:'<MoneyAmount v-model="v" />'})},l={render:()=>({components:{MoneyAmount:S},setup(){const e=r(5e4),h=r(!0);return{v:e,tax:h}},template:'<MoneyAmount v-model="v" v-model:tax-included="tax" currency="CNY" />'})};a.parameters={...a.parameters,docs:{...a.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      FigureNumberSearch
    },
    setup() {
      const v = ref('');
      return {
        v
      };
    },
    template: '<FigureNumberSearch v-model="v" />'
  })
}`,...a.parameters?.docs?.source}}};o.parameters={...o.parameters,docs:{...o.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      FigureNumberSearch
    },
    template: '<FigureNumberSearch model-value="DWG-001" disabled />'
  })
}`,...o.parameters?.docs?.source}}};t.parameters={...t.parameters,docs:{...t.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      BomTree
    },
    setup() {
      return {
        sampleBom
      };
    },
    template: '<BomTree :data="sampleBom" />'
  })
}`,...t.parameters?.docs?.source}}};s.parameters={...s.parameters,docs:{...s.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      BomTree
    },
    setup() {
      return {
        sampleBom
      };
    },
    template: '<BomTree :data="sampleBom" show-cost editable />'
  })
}`,...s.parameters?.docs?.source}}};n.parameters={...n.parameters,docs:{...n.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      ScanTrigger
    },
    setup() {
      const code = ref('');
      return {
        code
      };
    },
    template: '<ScanTrigger v-model="code" />'
  })
}`,...n.parameters?.docs?.source}}};c.parameters={...c.parameters,docs:{...c.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      MachineLoadBar
    },
    template: '<MachineLoadBar :percent="55" machine-name="CNC-01" />'
  })
}`,...c.parameters?.docs?.source}}};m.parameters={...m.parameters,docs:{...m.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      MachineLoadBar
    },
    template: '<MachineLoadBar :percent="78" machine-name="CNC-02" />'
  })
}`,...m.parameters?.docs?.source}}};p.parameters={...p.parameters,docs:{...p.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      MachineLoadBar
    },
    template: '<MachineLoadBar :percent="95" machine-name="CNC-03" />'
  })
}`,...p.parameters?.docs?.source}}};d.parameters={...d.parameters,docs:{...d.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      ApprovalChainRenderer
    },
    setup() {
      return {
        sampleChain
      };
    },
    template: '<ApprovalChainRenderer :nodes="sampleChain" />'
  })
}`,...d.parameters?.docs?.source}}};i.parameters={...i.parameters,docs:{...i.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      ApprovalChainRenderer
    },
    setup() {
      const nodes = sampleChain.map(n => ({
        ...n,
        status: 'APPROVED' as const
      }));
      return {
        nodes
      };
    },
    template: '<ApprovalChainRenderer :nodes="nodes" />'
  })
}`,...i.parameters?.docs?.source}}};u.parameters={...u.parameters,docs:{...u.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      MoneyAmount
    },
    setup() {
      const v = ref(1000);
      return {
        v
      };
    },
    template: '<MoneyAmount v-model="v" />'
  })
}`,...u.parameters?.docs?.source}}};l.parameters={...l.parameters,docs:{...l.parameters?.docs,source:{originalSource:`{
  render: () => ({
    components: {
      MoneyAmount
    },
    setup() {
      const v = ref(50000);
      const tax = ref(true);
      return {
        v,
        tax
      };
    },
    template: '<MoneyAmount v-model="v" v-model:tax-included="tax" currency="CNY" />'
  })
}`,...l.parameters?.docs?.source}}};const F=["FigureSearchBasic","FigureSearchDisabled","BomTreeReadonly","BomTreeWithCost","ScanTriggerUsb","MachineLoadGreen","MachineLoadYellow","MachineLoadRed","ApprovalPending","ApprovalApproved","MoneyCny","MoneyWithTax"];export{i as ApprovalApproved,d as ApprovalPending,t as BomTreeReadonly,s as BomTreeWithCost,a as FigureSearchBasic,o as FigureSearchDisabled,c as MachineLoadGreen,p as MachineLoadRed,m as MachineLoadYellow,u as MoneyCny,l as MoneyWithTax,n as ScanTriggerUsb,F as __namedExportsOrder,R as default};
