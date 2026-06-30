import { nextTick, onMounted, onUnmounted } from 'vue'
import { gsap, ScrollTrigger, prefersReducedMotion } from '../lib/gsap.js'

const DEFAULT_REVEAL_SELECTOR = [
  '.anim-1',
  '.anim-2',
  '.anim-3',
  '.anim-4',
  '.anim-5',
  '.anim-6',
  '[data-gsap-reveal]'
].join(', ')

export function usePageReveal(rootRef, options = {}) {
  let ctx

  function resetVisible(targets, props = 'opacity,visibility,transform') {
    if (targets.length === 0) return
    gsap.set(targets, {
      autoAlpha: 1,
      x: 0,
      y: 0,
      scale: 1,
      clearProps: props
    })
  }

  function fromTargets(targets, vars) {
    if (targets.length === 0) return
    gsap.from(targets, vars)
  }

  async function replayMotion() {
    await nextTick()
    if (!rootRef.value) return

    ctx?.revert()
    ctx = gsap.context(() => {
      const reduceMotion = prefersReducedMotion()
      const revealTargets = gsap.utils.toArray(options.revealSelector || DEFAULT_REVEAL_SELECTOR)
      const xTargets = options.xRevealSelector ? gsap.utils.toArray(options.xRevealSelector) : []
      const progressBars = gsap.utils.toArray(options.progressSelector || '.progress-fill')
      const verticalBars = gsap.utils.toArray(options.verticalBarSelector || '.bar-chart-bar')
      const donuts = gsap.utils.toArray(options.donutSelector || '.donut')
      const allTargets = [...revealTargets, ...xTargets, ...progressBars, ...verticalBars, ...donuts]

      if (reduceMotion) {
        if (allTargets.length === 0) return
        gsap.set(allTargets, {
          autoAlpha: 1,
          x: 0,
          y: 0,
          scale: 1,
          scaleX: 1,
          scaleY: 1,
          clearProps: 'transform,visibility'
        })
        return
      }

      fromTargets(revealTargets, {
        autoAlpha: 0,
        y: 18,
        scale: 0.985,
        stagger: 0.055,
        duration: 0.55,
        overwrite: 'auto',
        clearProps: 'opacity,visibility,transform',
        onInterrupt: () => resetVisible(revealTargets)
      })

      fromTargets(xTargets, {
        autoAlpha: 0,
        x: -18,
        stagger: 0.045,
        duration: 0.5,
        overwrite: 'auto',
        clearProps: 'opacity,visibility,transform',
        onInterrupt: () => resetVisible(xTargets)
      })

      fromTargets(progressBars, {
        scaleX: 0,
        transformOrigin: 'left center',
        duration: 0.75,
        stagger: 0.025,
        overwrite: 'auto',
        clearProps: 'transform'
      })

      fromTargets(verticalBars, {
        scaleY: 0,
        transformOrigin: 'bottom center',
        duration: 0.7,
        stagger: 0.035,
        overwrite: 'auto',
        clearProps: 'transform'
      })

      fromTargets(donuts, {
        '--pct': 0,
        duration: 0.85,
        stagger: 0.05,
        overwrite: 'auto'
      })
    }, rootRef.value)

    ScrollTrigger.refresh()
  }

  async function refreshMotion() {
    await nextTick()
    ScrollTrigger.refresh()
  }

  if (options.runOnMounted !== false) {
    onMounted(replayMotion)
  }
  onUnmounted(() => ctx?.revert())

  return { replayMotion, refreshMotion }
}
