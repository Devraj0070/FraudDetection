'use client';

import React, { Suspense, useEffect, useRef, useState } from 'react';
import { Canvas } from '@react-three/fiber';
import { PaymentCard } from './PaymentCard';
import { TransactionNetwork } from './TransactionNetwork';

export function HeroCanvas() {
  const pointer = useRef({ x: 0, y: 0 });
  const [hasWebGL, setHasWebGL] = useState(true);
  const [reducedMotion, setReducedMotion] = useState(false);

  useEffect(() => {
    // Check WebGL availability
    try {
      const canvas = document.createElement('canvas');
      const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
      if (!gl) setHasWebGL(false);
    } catch {
      setHasWebGL(false);
    }

    // Check prefers-reduced-motion
    if (typeof window !== 'undefined' && window.matchMedia) {
      const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
      setReducedMotion(motionQuery.matches);
      const listener = (e: MediaQueryListEvent) => setReducedMotion(e.matches);
      motionQuery.addEventListener('change', listener);
      return () => motionQuery.removeEventListener('change', listener);
    }
  }, []);

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
    const y = -(((e.clientY - rect.top) / rect.height) * 2 - 1);
    pointer.current = { x, y };
  };

  if (!hasWebGL) {
    return <StaticCardFallback />;
  }

  return (
    <div
      className="relative w-full h-[520px] md:h-[650px] flex items-center justify-center cursor-grab active:cursor-grabbing select-none"
      onPointerMove={handlePointerMove}
    >
      <Suspense fallback={<StaticCardFallback />}>
        <Canvas
          dpr={[1, 1.5]}
          camera={{ position: [0, 0, 5.2], fov: 42 }}
          gl={{ antialias: true, alpha: true, powerPreference: 'high-performance' }}
          className="w-full h-full"
        >
          {/* Lighting Rig */}
          <ambientLight intensity={0.5} />
          {/* Key light for metallic sheen */}
          <directionalLight position={[5, 8, 5]} intensity={1.8} color="#FFFFFF" />
          {/* Fill light from below */}
          <directionalLight position={[-5, -4, -2]} intensity={0.6} color="#333344" />
          {/* Electric lime rim edge light */}
          <spotLight
            position={[-4, 5, 3]}
            intensity={2.8}
            angle={0.6}
            penumbra={0.8}
            color="#D8FF00"
          />
          {/* Secondary lime accent back-light */}
          <pointLight position={[3, -2, 2]} intensity={1.5} color="#D8FF00" />

          {/* Central 3D Payment Card */}
          <PaymentCard pointer={pointer} reducedMotion={reducedMotion} />

          {/* Interconnected Transaction Network */}
          <TransactionNetwork reducedMotion={reducedMotion} />
        </Canvas>
      </Suspense>
    </div>
  );
}

function StaticCardFallback() {
  return (
    <div className="relative w-full h-[480px] flex items-center justify-center">
      <div className="w-[340px] md:w-[420px] h-[215px] md:h-[265px] rounded-2xl bg-gradient-to-br from-[#16171B] to-[#0A0A0C] border border-white/10 p-6 flex flex-col justify-between shadow-2xl relative overflow-hidden lime-glow">
        <div className="absolute top-0 left-0 right-0 h-1 bg-lime shadow-[0_0_12px_#D8FF00]" />
        <div className="flex justify-between items-center">
          <span className="font-display text-2xl tracking-wider text-white">SAFEPAY</span>
          <span className="font-mono text-xs text-lime uppercase tracking-widest px-2 py-0.5 rounded bg-lime/10 border border-lime/20">
            AI PLATINUM
          </span>
        </div>
        <div className="flex items-center gap-4 my-2">
          <div className="w-12 h-9 rounded bg-amber-400/80 border border-amber-300" />
          <div className="font-mono text-xs text-neutral-400">EMV SECURED</div>
        </div>
        <div>
          <div className="font-mono text-lg md:text-xl text-neutral-200 tracking-widest mb-3">
            4829 **** **** 9012
          </div>
          <div className="flex justify-between items-end text-xs font-mono">
            <div>
              <div className="text-neutral-500 text-[10px]">AUTHORIZED HOLDER</div>
              <div className="text-white font-medium">ALEXANDER VANCE</div>
            </div>
            <div>
              <div className="text-neutral-500 text-[10px]">VALID THRU</div>
              <div className="text-lime font-medium">12/29</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}