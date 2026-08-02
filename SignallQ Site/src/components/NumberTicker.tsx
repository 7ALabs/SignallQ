import { useEffect, useState } from 'react'

interface NumberTickerProps {
  value: number
  decimals?: number
  duration?: number
}

export function NumberTicker({ value, decimals = 0, duration = 800 }: NumberTickerProps) {
  const [current, setCurrent] = useState(0)

  useEffect(() => {
    let startTime: number | null = null
    let animationFrameId: number

    const step = (timestamp: number) => {
      if (!startTime) startTime = timestamp
      const progress = Math.min((timestamp - startTime) / duration, 1)
      
      // Easing out cubic: 1 - Math.pow(1 - progress, 3)
      const easeProgress = 1 - Math.pow(1 - progress, 3)
      
      setCurrent(value * easeProgress)

      if (progress < 1) {
        animationFrameId = requestAnimationFrame(step)
      } else {
        setCurrent(value)
      }
    }

    animationFrameId = requestAnimationFrame(step)

    return () => {
      cancelAnimationFrame(animationFrameId)
    }
  }, [value, duration])

  return <span>{current.toFixed(decimals)}</span>
}
