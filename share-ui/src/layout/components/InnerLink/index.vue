<template>
  <div :style="'height:' + height">
    <iframe
      :id="iframeId"
      style="width: 100%; height: 100%"
      :src="iframeSrc"
      frameborder="no"
    ></iframe>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute();
const props = defineProps({
  src: {
    type: String,
    default: "/"
  },
  iframeId: {
    type: String
  }
});

// 优先使用路由 meta.link（动态路由传入），fallback 到 props.src
const iframeSrc = computed(() => route.meta?.link || props.src);

const height = ref(document.documentElement.clientHeight - 94.5 + "px");
</script>
