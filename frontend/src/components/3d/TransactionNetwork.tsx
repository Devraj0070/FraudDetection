'use client';

import React, { useMemo, useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Line } from '@react-three/drei';
import * as THREE from 'three';

interface TransactionNetworkProps {
  reducedMotion?: boolean;
}

interface NodeData {
  pos: [number, number, number];
  size: number;
  label: string;
  isCore?: boolean;
}

export function TransactionNetwork({ reducedMotion }: TransactionNetworkProps) {
  const groupRef = useRef<THREE.Group>(null);
  const pulsesRef = useRef<THREE.InstancedMesh>(null);

  // Define 14 semantic nodes positioned in 3D orbit around the card
  const nodes: NodeData[] = useMemo(
    () => [
      { pos: [-2.6, 1.8, -0.6], size: 0.08, label: 'POS Terminal' },
      { pos: [-3.2, -0.4, 0.4], size: 0.1, label: 'Mobile Client' },
      { pos: [-2.2, -1.9, -0.4], size: 0.07, label: 'Web Checkout' },
      { pos: [2.5, 1.9, -0.5], size: 0.09, label: 'Cloud Gateway' },
      { pos: [3.3, 0.2, 0.5], size: 0.11, label: 'Risk Engine', isCore: true },
      { pos: [2.6, -1.8, -0.3], size: 0.08, label: 'Settlement Vault' },
      { pos: [0.0, 2.5, -0.8], size: 0.09, label: 'Token Vault' },
      { pos: [0.0, -2.4, -0.6], size: 0.08, label: 'Idempotency Store' },
      { pos: [-1.4, 2.2, 0.6], size: 0.06, label: 'Biometric Auth' },
      { pos: [1.6, 2.3, 0.7], size: 0.07, label: 'Random Forest' },
      { pos: [-1.7, -2.2, 0.5], size: 0.06, label: 'Rule Engine' },
      { pos: [1.8, -2.3, 0.6], size: 0.07, label: 'Pessimistic Lock' },
      { pos: [-3.6, 1.0, -0.2], size: 0.05, label: 'Relay Alpha' },
      { pos: [3.6, -0.9, -0.1], size: 0.05, label: 'Relay Beta' },
    ],
    []
  );

  // Generate connection pairs between adjacent/logical nodes
  const connections: [number, number][] = useMemo(
    () => [
      [0, 8], [8, 6], [6, 9], [9, 3],
      [0, 12], [12, 1], [1, 10], [10, 7],
      [3, 4], [4, 13], [13, 5], [5, 11], [11, 7],
      [1, 0], [4, 5], [6, 4], [8, 10],
      [9, 11], [10, 2], [2, 7]
    ],
    []
  );

  // Setup animated data packets traveling along connections
  const packetCount = connections.length * 2;
  const dummy = useMemo(() => new THREE.Object3D(), []);
  const packetProgress = useMemo(
    () => Array.from({ length: packetCount }, (_, i) => (i / packetCount) * Math.PI * 2),
    [packetCount]
  );

  useFrame((state, delta) => {
    if (!groupRef.current) return;

    if (!reducedMotion) {
      const t = state.clock.getElapsedTime();
      // Slow orbital drift
      groupRef.current.rotation.y = t * 0.08;
      groupRef.current.rotation.x = Math.sin(t * 0.05) * 0.04;

      // Animate packet positions along line segments
      if (pulsesRef.current) {
        for (let i = 0; i < packetCount; i++) {
          packetProgress[i] += delta * 0.6;
          const cycle = (packetProgress[i] % 1 + 1) % 1;

          const connIdx = i % connections.length;
          const [startIdx, endIdx] = connections[connIdx];
          const p1 = nodes[startIdx].pos;
          const p2 = nodes[endIdx].pos;

          const px = p1[0] + (p2[0] - p1[0]) * cycle;
          const py = p1[1] + (p2[1] - p1[1]) * cycle;
          const pz = p1[2] + (p2[2] - p1[2]) * cycle;

          dummy.position.set(px, py, pz);
          dummy.scale.setScalar(0.045);
          dummy.updateMatrix();
          pulsesRef.current.setMatrixAt(i, dummy.matrix);
        }
        pulsesRef.current.instanceMatrix.needsUpdate = true;
      }
    }
  });

  return (
    <group ref={groupRef}>
      {/* 1. Network Nodes (Geometric Spheres & Wireframes) */}
      {nodes.map((node, i) => (
        <group key={i} position={node.pos}>
          {/* Node Core */}
          <mesh>
            <sphereGeometry args={[node.size, 16, 16]} />
            <meshStandardMaterial
              color={node.isCore ? '#D8FF00' : '#FFFFFF'}
              emissive={node.isCore ? '#D8FF00' : '#444444'}
              emissiveIntensity={node.isCore ? 2.0 : 0.4}
              roughness={0.2}
              metalness={0.8}
            />
          </mesh>

          {/* Orbiting Wireframe Diamond */}
          <mesh rotation={[Math.PI / 4, Math.PI / 4, 0]}>
            <octahedronGeometry args={[node.size * 1.5, 0]} />
            <meshBasicMaterial
              color="#D8FF00"
              wireframe={true}
              transparent={true}
              opacity={0.35}
            />
          </mesh>
        </group>
      ))}

      {/* 2. Interconnecting Lines */}
      {connections.map(([start, end], idx) => (
        <Line
          key={idx}
          points={[nodes[start].pos, nodes[end].pos]}
          color="#D8FF00"
          opacity={0.18}
          transparent={true}
          lineWidth={1}
        />
      ))}

      {/* 3. Instanced Moving Data Packets */}
      <instancedMesh ref={pulsesRef} args={[undefined, undefined, packetCount]}>
        <sphereGeometry args={[1, 8, 8]} />
        <meshBasicMaterial color="#D8FF00" />
      </instancedMesh>
    </group>
  );
}