<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <span class="footer-brand">BJTU Course Review</span>
      <span class="footer-dot">·</span>
      <span>北京交通大学课程评价系统</span>
      <span class="footer-dot">·</span>
      <span class="footer-muted">让选课更明智</span>
    </div>
  </footer>
</template>

<script setup></script>

<style scoped>
.app-footer {
  height: 56px;
  background: #1a1f36;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
}

.footer-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}

.footer-brand {
  font-weight: 700;
  background: var(--brand-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.5px;
}

.footer-dot {
  opacity: 0.4;
}

.footer-muted {
  opacity: 0.6;
}
</style>
