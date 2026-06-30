<template>
  <div class="mindmap-wrap">
    <div class="mindmap-toolbar">
      <button type="button" class="mindmap-btn" title="放大" @click="zoomBy(1.25)">
        <i class="ri-zoom-in-line"></i>
      </button>
      <button type="button" class="mindmap-btn" title="缩小" @click="zoomBy(0.8)">
        <i class="ri-zoom-out-line"></i>
      </button>
      <button type="button" class="mindmap-btn" title="适应窗口" @click="fit">
        <i class="ri-fullscreen-fit-line"></i>
      </button>
    </div>
    <svg ref="svgRef" class="mindmap-svg"></svg>
    <div v-if="!hasContent" class="mindmap-empty">暂无可视化的思维导图数据</div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Transformer } from 'markmap-lib'
import { Markmap } from 'markmap-view'

const props = defineProps({
  // markmap 直接消费的 Markdown 文本（# 根 / ## 分支 / - 要点）
  markdown: { type: String, default: '' }
})

const svgRef = ref(null)
const hasContent = ref(false)
const transformer = new Transformer()
let mm = null

function render() {
  const md = (props.markdown || '').trim()
  hasContent.value = Boolean(md)
  if (!svgRef.value) return
  if (!md) {
    if (mm) mm.setData({ content: '', children: [] })
    return
  }
  const { root } = transformer.transform(md)
  if (!mm) {
    mm = Markmap.create(svgRef.value, {
      autoFit: true,
      duration: 320,
      paddingX: 16,
      spacingVertical: 8,
      spacingHorizontal: 90,
      initialExpandLevel: -1
    })
  }
  mm.setData(root)
  mm.fit()
}

function zoomBy(factor) {
  if (!mm) return
  mm.rescale(factor)
}

function fit() {
  if (mm) mm.fit()
}

onMounted(render)
watch(() => props.markdown, () => render())
onBeforeUnmount(() => {
  if (mm) {
    mm.destroy()
    mm = null
  }
})
</script>

<style scoped>
.mindmap-wrap {
  position: relative;
  width: 100%;
  height: min(60vh, 520px);
  border: 1px solid var(--border);
  border-radius: 10px;
  background:
    radial-gradient(circle at 1px 1px, rgba(91, 141, 239, 0.12) 1px, transparent 0);
  background-size: 22px 22px;
  background-color: var(--card-bg-solid);
  overflow: hidden;
}
.mindmap-svg {
  width: 100%;
  height: 100%;
  display: block;
}
.mindmap-svg :deep(.markmap-node) {
  cursor: pointer;
}
.mindmap-toolbar {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  display: flex;
  gap: 6px;
}
.mindmap-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--card-bg-solid);
  color: var(--text-secondary);
  cursor: pointer;
  display: grid;
  place-items: center;
  font-size: 1rem;
  transition: all 0.18s;
}
.mindmap-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.mindmap-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--text-muted);
  font-size: 0.9rem;
}
</style>
