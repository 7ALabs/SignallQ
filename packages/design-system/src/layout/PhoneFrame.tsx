import React from 'react';
import { LK } from '../tokens.js';

export interface PhoneFrameProps {
  children?: React.ReactNode;
}

/** 390×820 device frame for previewing screens in the design system viewer. */
export function PhoneFrame({ children }: PhoneFrameProps) {
  return (
    <div
      style={{
        width: 390,
        height: 820,
        background: LK.bgPrimary,
        borderRadius: 36,
        border: '10px solid #111',
        boxShadow: '0 30px 80px rgba(0,0,0,.28)',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        fontFamily: LK.font,
      }}
    >
      {children}
      <div
        style={{
          position: 'absolute',
          bottom: 6,
          left: '50%',
          transform: 'translateX(-50%)',
          width: 120,
          height: 4,
          borderRadius: 2,
          background: LK.textPrimary,
          opacity: 0.25,
        }}
      />
    </div>
  );
}
