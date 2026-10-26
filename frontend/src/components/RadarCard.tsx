import {
  Radar,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  ResponsiveContainer,
} from 'recharts'

export default function RadarCard({ report }: { report: any }) {
  const data = [
    { subject: 'Quality', A: report.qualityScore },
    { subject: 'Security', A: report.securityScore },
    { subject: 'Testing', A: report.testingScore },
    { subject: 'Performance', A: report.performanceScore },
    { subject: 'Maintain', A: report.maintainabilityScore },
  ]
  return (
    <div className="card" style={{ height: 340 }}>
      <div style={{ fontWeight: 600, marginBottom: 8 }}>Engineering Health Dimensions</div>
      <div style={{ width: '100%', height: 270 }}>
        <ResponsiveContainer width="100%" height="100%">
          <RadarChart data={data}>
            <PolarGrid stroke="#333" />
            <PolarAngleAxis dataKey="subject" tick={{ fill: '#9ca3af', fontSize: 12 }} />
            <PolarRadiusAxis domain={[0, 100]} tick={{ fill: '#666', fontSize: 10 }} />
            <Radar dataKey="A" stroke="#8b5cf6" fill="#8b5cf6" fillOpacity={0.5} />
          </RadarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}