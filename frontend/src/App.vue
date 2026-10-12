<script setup>
import { computed, onMounted, ref } from 'vue'
const keyword=ref(''), status=ref(''), page=ref(1), total=ref(0), items=ref([]), summary=ref({}), loading=ref(false), error=ref(''), detail=ref(null), detailError=ref('')
const pages=computed(()=>Math.max(1,Math.ceil(total.value/8)))
const money=value=>new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY',maximumFractionDigits:2}).format(value??0)
const statusClass=value=>({'待处理':'pending','处理中':'processing','已完成':'completed','已取消':'cancelled'}[value])
let requestId=0
async function request(url){const res=await fetch(url);if(!res.ok) throw new Error('服务暂时不可用，请检查后端是否启动后重试。');return res.json()}
async function load(reset=false){if(reset)page.value=1;const id=++requestId;loading.value=true;error.value='';try{const data=await request('/api/orders?'+new URLSearchParams({keyword:keyword.value,status:status.value,page:page.value,size:8}));if(id!==requestId)return;items.value=data.items;total.value=data.total}catch(e){if(id===requestId)error.value=e.message}finally{if(id===requestId)loading.value=false}}
async function refresh(){load();try{summary.value=await request('/api/summary')}catch(e){error.value=e.message}}
async function openDetail(row){detailError.value='';try{detail.value=await request('/api/orders/'+row.ID)}catch(e){detailError.value=e.message}}
function reset(){keyword.value='';status.value='';load(true)}
function go(p){page.value=p;load()}
onMounted(refresh)
</script>

<template>
  <div class="layout">
    <aside class="sidebar"><a class="brand" href="/"> <span class="logo">简</span><span>简查<span class="brand-sub">BUSINESS INSIGHT</span></span></a><div class="nav-label">工作空间</div><div class="nav-item active"><span>▦</span> 业务查询 <span class="nav-dot"></span></div><div class="sidebar-bottom"><span class="avatar">演</span><div>演示工作空间<small>Spring Boot + Vue</small></div></div></aside>
    <main>
      <header><div class="breadcrumb">工作空间 <span>/</span> <strong>业务查询</strong></div><span class="demo-tag">演示环境</span></header>
      <section class="content">
        <div class="page-heading"><div><div class="eyebrow">BUSINESS OVERVIEW</div><h1>业务查询</h1><p>快速检索业务订单，让每一笔业务清晰可见。</p></div><button class="button secondary" @click="refresh" :disabled="loading">↻ 刷新数据</button></div>
        <div class="stats"><div class="stat"><span>订单总数</span><strong>{{ summary.TOTAL ?? '—' }}<small>笔</small></strong><div class="stat-caption">全部业务订单</div></div><div class="stat"><span>订单总金额</span><strong class="amount">{{ summary.REVENUE == null ? '—' : money(summary.REVENUE) }}</strong><div class="stat-caption">包含所有订单状态</div></div><div class="stat"><span>待处理订单</span><strong>{{ summary.PENDING ?? '—' }}<small>笔</small></strong><div class="stat-caption"><i class="orange-dot"></i>等待业务跟进</div></div><div class="stat"><span>合作客户</span><strong>{{ summary.CUSTOMERS ?? '—' }}<small>家</small></strong><div class="stat-caption">按客户名称去重</div></div></div>
        <div class="panel">
          <form class="filters" @submit.prevent="load(true)"><div class="field keyword"><label for="keyword">关键词查询</label><div class="input-wrap"><span>⌕</span><input id="keyword" v-model="keyword" maxlength="100" placeholder="输入订单号、客户名称或产品名称"></div></div><div class="field"><label for="status">订单状态</label><select id="status" v-model="status"><option value="">全部状态</option><option>待处理</option><option>处理中</option><option>已完成</option><option>已取消</option></select></div><button class="button primary" :disabled="loading" type="submit">查询</button><button class="button secondary" type="button" @click="reset">重置</button></form>
          <div class="table-title"><h2>订单列表 <span>{{ total }}</span></h2><small>金额单位：人民币</small></div>
          <div v-if="error" class="message error" role="alert">{{ error }} <button @click="refresh">重试</button></div>
          <div v-else-if="loading" class="message" role="status">正在加载订单…</div>
          <div v-else class="table-wrap"><table><thead><tr><th>订单编号</th><th>客户名称</th><th>产品 / 服务</th><th class="numeric">订单金额</th><th>状态</th><th>创建日期</th><th>操作</th></tr></thead><tbody><tr v-for="row in items" :key="row.ID"><td class="order-no">{{ row.ORDER_NO }}</td><td>{{ row.CUSTOMER }}</td><td class="muted">{{ row.PRODUCT }}</td><td class="numeric money">{{ money(row.AMOUNT) }}</td><td><span class="status" :class="statusClass(row.STATUS)"><i></i>{{ row.STATUS }}</span></td><td class="muted">{{ row.CREATED_AT }}</td><td><button class="detail-link" @click="openDetail(row)">查看详情</button></td></tr><tr v-if="!items.length"><td colspan="7" class="empty">没有找到符合条件的订单，请调整查询条件。</td></tr></tbody></table></div>
          <div v-if="detailError" class="message error" role="alert">{{ detailError }}</div>
          <div class="pagination"><span>共 {{ total }} 条记录 · 每页 8 条</span><div><button :disabled="page===1 || loading" @click="go(page-1)" aria-label="上一页">‹</button><span class="current-page">{{ page }}</span><span class="page-count">/ {{ pages }}</span><button :disabled="page>=pages || loading" @click="go(page+1)" aria-label="下一页">›</button></div></div>
        </div>
        <footer><span><i class="green-dot"></i>内置演示数据 · 仅供功能体验</span><span>简查业务工作台</span></footer>
      </section>
    </main>
    <div v-if="detail" class="modal-backdrop" @click.self="detail=null" @keydown.esc="detail=null"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="detail-title" tabindex="-1"><div class="modal-heading"><div><div class="eyebrow">ORDER DETAILS</div><h2 id="detail-title">订单详情</h2></div><button autofocus class="close" @click="detail=null" aria-label="关闭详情">×</button></div><div class="detail-banner"><strong>{{ detail.ORDER_NO }}</strong><span class="status" :class="statusClass(detail.STATUS)">{{ detail.STATUS }}</span></div><dl><dt>客户名称</dt><dd>{{ detail.CUSTOMER }}</dd><dt>产品 / 服务</dt><dd>{{ detail.PRODUCT }}</dd><dt>订单金额</dt><dd class="detail-amount">{{ money(detail.AMOUNT) }}</dd><dt>创建日期</dt><dd>{{ detail.CREATED_AT }}</dd><dt>联系人</dt><dd>{{ detail.CONTACT }}</dd><dt>业务备注</dt><dd>{{ detail.NOTE }}</dd></dl><button class="button primary modal-done" @click="detail=null">关闭详情</button></section></div>
  </div>
</template>
