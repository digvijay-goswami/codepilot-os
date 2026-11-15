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
    <div className="radar" role="img" aria-label="Radar chart of the five health scores">
      <ResponsiveContainer width="100%" height="100%">
        <RadarChart data={data} outerRadius="72%">
          <PolarGrid stroke="#243650" />
          <PolarAngleAxis dataKey="subject" tick={{ fill: '#93a3bb', fontSize: 12 }} />
          <PolarRadiusAxis domain={[0, 100]} tick={false} axisLine={false} />
          <Radar dataKey="A" stroke="#8aa2ff" strokeWidth={2} fill="#8aa2ff" fillOpacity={0.28} />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  )
}
