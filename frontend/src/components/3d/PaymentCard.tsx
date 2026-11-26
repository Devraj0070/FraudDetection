'use client';

import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Text } from '@react-three/drei';
import * as THREE from 'three';

interface PaymentCardProps {
  pointer: React.MutableRefObject<{ x: number; y: number }>;
  reducedMotion?: boolean;
}

export function PaymentCard({ pointer, reducedMotion }: PaymentCardProps) {
  const groupRef = useRef<THREE.Group>(null);
  const cardRef = useRef<THREE.Mesh>(null);

  // Floating and gentle interactive tilt animation
  useFrame((state, delta) => {
    if (!groupRef.current) return;

    if (!reducedMotion) {
      const t = state.clock.getElapsedTime();
      // Gentle floating sine motion
      groupRef.current.position.y = Math.sin(t * 1.2) * 0.12;

      // Mouse interactive tilt lerp
      const targetRotX = pointer.current.y * 0.35 + Math.sin(t * 0.8) * 0.05;
      const targetRotY = pointer.current.x * 0.55 + Math.cos(t * 0.6) * 0.08;

      groupRef.current.rotation.x = THREE.MathUtils.lerp(groupRef.current.rotation.x, targetRotX, delta * 3);
      groupRef.current.rotation.y = THREE.MathUtils.lerp(groupRef.current.rotation.y, targetRotY, delta * 3);
      groupRef.current.rotation.z = THREE.MathUtils.lerp(
        groupRef.current.rotation.z,
        -pointer.current.x * 0.15,
        delta * 3
      );
    }
  });

  return (
    <group ref={groupRef} position={[0, 0, 0]}>
      {/* 1. Main Card Body (Standard ISO/IEC 7810 ratio ~ 1.586, scaled: width 3.6, height 2.27, depth 0.06) */}
      <mesh ref={cardRef} castShadow receiveShadow position={[0, 0, 0]}>
        <boxGeometry args={[3.6, 2.27, 0.06]} />
        <meshPhysicalMaterial
          color="#0c0d10"
          roughness={0.25}
          metalness={0.88}
          clearcoat={0.6}
          clearcoatRoughness={0.15}
          reflectivity={0.9}
        />
      </mesh>

      {/* 2. Top Edge Electric Lime Accent Strip */}
      <mesh position={[0, 1.11, 0.032]}>
        <boxGeometry args={[3.56, 0.03, 0.005]} />
        <meshStandardMaterial
          color="#D8FF00"
          emissive="#D8FF00"
          emissiveIntensity={1.8}
          roughness={0.1}
        />
      </mesh>

      {/* 3. Bottom Corner Neon Core Glow Strip */}
      <mesh position={[1.5, -0.95, 0.032]}>
        <boxGeometry args={[0.4, 0.04, 0.005]} />
        <meshStandardMaterial
          color="#D8FF00"
          emissive="#D8FF00"
          emissiveIntensity={2.5}
          roughness={0.1}
        />
      </mesh>

      {/* 4. Abstract Metallic EMV Security Chip */}
      <group position={[-1.15, 0.35, 0.035]}>
        {/* Chip Base Plate */}
        <mesh>
          <boxGeometry args={[0.55, 0.45, 0.015]} />
          <meshStandardMaterial
            color="#E5C158"
            metalness={0.95}
            roughness={0.2}
          />
        </mesh>
        {/* Chip Pattern Circuit Traces */}
        <mesh position={[0, 0, 0.009]}>
          <planeGeometry args={[0.5, 0.4]} />
          <meshBasicMaterial color="#9C7D2B" wireframe={true} />
        </mesh>
        {/* Contactless Signal Icon (Subtle Curved Rings) */}
        <group position={[0.48, 0, 0]}>
          <mesh rotation={[0, 0, -Math.PI / 4]}>
            <torusGeometry args={[0.1, 0.01, 8, 24, Math.PI / 2]} />
            <meshBasicMaterial color="#FFFFFF" opacity={0.6} transparent />
          </mesh>
          <mesh rotation={[0, 0, -Math.PI / 4]}>
            <torusGeometry args={[0.18, 0.01, 8, 24, Math.PI / 2]} />
            <meshBasicMaterial color="#FFFFFF" opacity={0.4} transparent />
          </mesh>
        </group>
      </group>

      {/* 5. Holographic SafePay Security Core (3D Geometric Diamond Badge) */}
      <mesh position={[1.25, 0.65, 0.036]} rotation={[0, 0, Math.PI / 4]}>
        <planeGeometry args={[0.35, 0.35]} />
        <meshPhysicalMaterial
          color="#D8FF00"
          emissive="#D8FF00"
          emissiveIntensity={0.8}
          roughness={0.1}
          metalness={0.9}
          clearcoat={1.0}
        />
      </mesh>

      {/* 6. 3D Card Typography */}
      {/* Brand Header */}
      <Text
        position={[-1.48, 0.85, 0.036]}
        fontSize={0.22}
        color="#FFFFFF"
        font="https://fonts.gstatic.com/s/oswald/v49/TK3iWkUHHAIjg752GT8G.woff"
        anchorX="left"
        anchorY="middle"
        letterSpacing={0.06}
      >
        SAFEPAY
      </Text>

      {/* Sub-label */}
      <Text
        position={[-0.45, 0.84, 0.036]}
        fontSize={0.08}
        color="#D8FF00"
        anchorX="left"
        anchorY="middle"
        letterSpacing={0.1}
      >
        AI PLATINUM
      </Text>

      {/* Card Number (Fictional decorative display) */}
      <Text
        position={[-1.48, -0.22, 0.036]}
        fontSize={0.19}
        color="#F0F0F0"
        font="https://fonts.gstatic.com/s/jetbrainsmono/v18/tDbY2o-flEEny0FZhsfKu5WU4zr3E_BX0PnT8RD8yKxjPVmUqiDC.woff"
        anchorX="left"
        anchorY="middle"
        letterSpacing={0.12}
      >
        4829  ****  ****  9012
      </Text>

      {/* Cardholder Name */}
      <Text
        position={[-1.48, -0.68, 0.036]}
        fontSize={0.1}
        color="#AAAAAA"
        anchorX="left"
        anchorY="middle"
        letterSpacing={0.08}
      >
        AUTHORIZED HOLDER
      </Text>
      <Text
        position={[-1.48, -0.84, 0.036]}
        fontSize={0.13}
        color="#FFFFFF"
        anchorX="left"
        anchorY="middle"
        letterSpacing={0.05}
      >
        ALEXANDER VANCE
      </Text>

      {/* Expiry & Chip Label */}
      <Text
        position={[0.55, -0.68, 0.036]}
        fontSize={0.08}
        color="#AAAAAA"
        anchorX="left"
        anchorY="middle"
        letterSpacing={0.05}
      >
        VALID THRU
      </Text>
      <Text
        position={[0.55, -0.84, 0.036]}
        fontSize={0.13}
        color="#D8FF00"
        font="https://fonts.gstatic.com/s/jetbrainsmono/v18/tDbY2o-flEEny0FZhsfKu5WU4zr3E_BX0PnT8RD8yKxjPVmUqiDC.woff"
        anchorX="left"
        anchorY="middle"
        letterSpacing={0.05}
      >
        12/29
      </Text>

      {/* 7. Subtle Ambient Backlight Glow behind the Card */}
      <pointLight position={[0, 0, -0.4]} color="#D8FF00" intensity={1.5} distance={3.5} />
    </group>
  );
}