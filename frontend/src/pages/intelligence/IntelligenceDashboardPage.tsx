import { useState } from 'react';
import { PageHeader } from '@/components/shared/PageHeader';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  useAnomalies,
  useReviewAnomaly,
  useErrorResolutionStats,
  useErrorResolutions,
  useApproveResolution,
  useProcessMiningInsights,
  useProcessMiningMetrics,
  useStpRate,
  useTriggerAnalysis,
  useForecast,
  useForecastHistory,
  useGenerateForecast,
} from '@/hooks/use-intelligence';
import { Skeleton } from '@/components/ui/skeleton';

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  if (ms < 60_000) return `${(ms / 1000).toFixed(1)}s`;
  if (ms < 3_600_000) return `${(ms / 60_000).toFixed(1)}m`;
  return `${(ms / 3_600_000).toFixed(1)}h`;
}

function AnomalyScoreBadge({ score }: { score: number }) {
  const variant = score >= 0.9 ? 'destructive' : score >= 0.7 ? 'default' : 'secondary';
  return <Badge variant={variant}>{(score * 100).toFixed(0)}%</Badge>;
}

function AnomalyStatusBadge({ status }: { status: string }) {
  const variant =
    status === 'CONFIRMED_FRAUD'
      ? 'destructive'
      : status === 'FLAGGED'
        ? 'default'
        : status === 'UNDER_REVIEW'
          ? 'outline'
          : 'secondary';
  return <Badge variant={variant}>{status.replace('_', ' ')}</Badge>;
}

function AnomaliesTab() {
  const { data: anomalies, isLoading } = useAnomalies();
  const reviewMutation = useReviewAnomaly();

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Flagged Anomalies</CardTitle>
        <CardDescription>Endorsements flagged by the anomaly detection engine</CardDescription>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Employer</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Score</TableHead>
              <TableHead>Explanation</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Flagged At</TableHead>
              <TableHead>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {anomalies && anomalies.length > 0 ? (
              anomalies.map((anomaly) => (
                <TableRow key={anomaly.id}>
                  <TableCell className="font-mono text-xs">
                    {anomaly.employerId.slice(0, 8)}...
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{anomaly.anomalyType.replace('_', ' ')}</Badge>
                  </TableCell>
                  <TableCell>
                    <AnomalyScoreBadge score={anomaly.score} />
                  </TableCell>
                  <TableCell className="max-w-xs truncate text-sm">{anomaly.explanation}</TableCell>
                  <TableCell>
                    <AnomalyStatusBadge status={anomaly.status} />
                  </TableCell>
                  <TableCell className="text-sm">
                    {new Date(anomaly.flaggedAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell>
                    {anomaly.status === 'FLAGGED' && (
                      <div className="flex gap-1">
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={reviewMutation.isPending}
                          onClick={() =>
                            reviewMutation.mutate({
                              id: anomaly.id,
                              status: 'UNDER_REVIEW',
                              notes: 'Under investigation',
                            })
                          }
                        >
                          Review
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          disabled={reviewMutation.isPending}
                          onClick={() =>
                            reviewMutation.mutate({
                              id: anomaly.id,
                              status: 'DISMISSED',
                              notes: 'False positive',
                            })
                          }
                        >
                          Dismiss
                        </Button>
                      </div>
                    )}
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={7} className="text-muted-foreground text-center">
                  No anomalies detected
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}

function ForecastsTab() {
  const [employerId, setEmployerId] = useState('11111111-1111-1111-1111-111111111111');
  const [insurerId, setInsurerId] = useState('22222222-2222-2222-2222-222222222222');

  const { data: forecast, isLoading: forecastLoading } = useForecast(employerId, insurerId);
  const { data: history, isLoading: historyLoading } = useForecastHistory(employerId);
  const generateMutation = useGenerateForecast();

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Balance Forecast</CardTitle>
          <CardDescription>30-day EA balance projections per employer</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-4">
            <div className="space-y-1">
              <Label className="text-xs">Employer ID</Label>
              <Input
                value={employerId}
                onChange={(e) => setEmployerId(e.target.value)}
                className="w-72 text-xs"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Insurer ID</Label>
              <Input
                value={insurerId}
                onChange={(e) => setInsurerId(e.target.value)}
                className="w-72 text-xs"
              />
            </div>
            <div className="flex items-end">
              <Button
                size="sm"
                onClick={() => generateMutation.mutate({ employerId, insurerId })}
                disabled={generateMutation.isPending}
              >
                {generateMutation.isPending ? 'Generating...' : 'Generate Forecast'}
              </Button>
            </div>
          </div>

          {forecastLoading ? (
            <Skeleton className="h-32 w-full" />
          ) : forecast ? (
            <div className="grid gap-4 md:grid-cols-4">
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Forecasted Amount</CardDescription>
                  <CardTitle className="text-2xl">
                    ₹{forecast.forecastedAmount.toLocaleString('en-IN')}
                  </CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Actual Amount</CardDescription>
                  <CardTitle className="text-2xl">
                    {forecast.actualAmount != null
                      ? `₹${forecast.actualAmount.toLocaleString('en-IN')}`
                      : '--'}
                  </CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Accuracy</CardDescription>
                  <CardTitle className="text-2xl">
                    {forecast.accuracy != null
                      ? `${(forecast.accuracy * 100).toFixed(1)}%`
                      : '--'}
                  </CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Forecast Date</CardDescription>
                  <CardTitle className="text-lg">
                    {new Date(forecast.forecastDate).toLocaleDateString()}
                  </CardTitle>
                </CardHeader>
              </Card>
            </div>
          ) : (
            <p className="text-muted-foreground text-center py-4">
              No forecast data available. Click Generate to create one.
            </p>
          )}

          {forecast?.narrative && (
            <div className="rounded-lg border p-3">
              <p className="text-sm font-medium mb-1">Narrative</p>
              <p className="text-muted-foreground text-sm">{forecast.narrative}</p>
            </div>
          )}
        </CardContent>
      </Card>

      {historyLoading ? (
        <Skeleton className="h-48 w-full" />
      ) : history && history.length > 0 ? (
        <Card>
          <CardHeader>
            <CardTitle>Forecast History</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead className="text-right">Forecasted</TableHead>
                  <TableHead className="text-right">Actual</TableHead>
                  <TableHead className="text-right">Accuracy</TableHead>
                  <TableHead>Narrative</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {history.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell className="text-sm">
                      {new Date(item.forecastDate).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₹{item.forecastedAmount.toLocaleString('en-IN')}
                    </TableCell>
                    <TableCell className="text-right">
                      {item.actualAmount != null
                        ? `₹${item.actualAmount.toLocaleString('en-IN')}`
                        : '--'}
                    </TableCell>
                    <TableCell className="text-right">
                      {item.accuracy != null
                        ? `${(item.accuracy * 100).toFixed(1)}%`
                        : '--'}
                    </TableCell>
                    <TableCell className="max-w-xs truncate text-xs">
                      {item.narrative}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}

function ErrorResolutionTab() {
  const { data: stats, isLoading: statsLoading } = useErrorResolutionStats();
  const { data: resolutions, isLoading: resLoading } = useErrorResolutions();
  const approveMutation = useApproveResolution();

  if (statsLoading || resLoading) return <Skeleton className="h-64 w-full" />;

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Total Resolutions</CardDescription>
            <CardTitle className="text-2xl">{stats?.totalResolutions ?? 0}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Auto-Applied</CardDescription>
            <CardTitle className="text-2xl">{stats?.autoApplied ?? 0}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Suggested</CardDescription>
            <CardTitle className="text-2xl">{stats?.suggested ?? 0}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Auto-Apply Rate</CardDescription>
            <CardTitle className="text-2xl">{(stats?.autoApplyRate ?? 0).toFixed(1)}%</CardTitle>
          </CardHeader>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Recent Resolutions</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Error Type</TableHead>
                <TableHead>Original</TableHead>
                <TableHead>Corrected</TableHead>
                <TableHead>Confidence</TableHead>
                <TableHead>Auto-Applied</TableHead>
                <TableHead>Created</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {resolutions && resolutions.length > 0 ? (
                resolutions.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell>
                      <Badge variant="outline">{r.errorType}</Badge>
                    </TableCell>
                    <TableCell className="max-w-xs truncate text-xs font-mono">
                      {r.originalValue}
                    </TableCell>
                    <TableCell className="max-w-xs truncate text-xs font-mono">
                      {r.correctedValue}
                    </TableCell>
                    <TableCell>{(r.confidence * 100).toFixed(0)}%</TableCell>
                    <TableCell>
                      <Badge variant={r.autoApplied ? 'default' : 'secondary'}>
                        {r.autoApplied ? 'Yes' : 'No'}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-sm">
                      {new Date(r.createdAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell>
                      {!r.autoApplied && (
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={approveMutation.isPending}
                          onClick={() => approveMutation.mutate(r.id)}
                        >
                          {approveMutation.isPending ? 'Approving...' : 'Approve'}
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={7} className="text-muted-foreground text-center">
                    No error resolutions yet
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}

function ProcessMiningTab() {
  const { data: insights, isLoading: insightsLoading } = useProcessMiningInsights();
  const { data: metrics, isLoading: metricsLoading } = useProcessMiningMetrics();
  const { data: stpRate, isLoading: stpLoading } = useStpRate();
  const triggerMutation = useTriggerAnalysis();

  if (insightsLoading || stpLoading) return <Skeleton className="h-64 w-full" />;

  const maxAvgDuration = metrics?.reduce(
    (max, m) => Math.max(max, m.avgDurationMs),
    0,
  ) ?? 1;

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Overall STP Rate</CardDescription>
            <CardTitle className="text-3xl">
              {(stpRate?.overallStpRate ?? 0).toFixed(1)}%
            </CardTitle>
          </CardHeader>
        </Card>
        {stpRate?.perInsurerStpRate &&
          Object.entries(stpRate.perInsurerStpRate).map(([insurerId, rate]) => (
            <Card key={insurerId}>
              <CardHeader className="pb-2">
                <CardDescription>Insurer {insurerId.slice(0, 8)}...</CardDescription>
                <CardTitle className="text-2xl">{Number(rate).toFixed(1)}%</CardTitle>
              </CardHeader>
            </Card>
          ))}
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Bottleneck Insights</CardTitle>
            <CardDescription>Workflow bottlenecks identified by process mining</CardDescription>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => triggerMutation.mutate()}
            disabled={triggerMutation.isPending}
          >
            {triggerMutation.isPending ? 'Analyzing...' : 'Run Analysis'}
          </Button>
        </CardHeader>
        <CardContent>
          {insights && insights.length > 0 ? (
            <div className="space-y-3">
              {insights.map((insight, idx) => (
                <div key={idx} className="rounded-lg border p-3">
                  <div className="mb-1 flex items-center gap-2">
                    <Badge variant="destructive">{insight.insightType}</Badge>
                    <span className="text-sm font-medium">{insight.insurerName}</span>
                  </div>
                  <p className="text-muted-foreground text-sm">{insight.insight}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-muted-foreground text-center">
              No bottlenecks detected. Run analysis to generate insights.
            </p>
          )}
        </CardContent>
      </Card>

      {/* Process Mining Metrics Table */}
      {metricsLoading ? (
        <Skeleton className="h-48 w-full" />
      ) : metrics && metrics.length > 0 ? (
        <Card>
          <CardHeader>
            <CardTitle>Transition Metrics</CardTitle>
            <CardDescription>
              Average durations for status transitions across all insurers
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>From Status</TableHead>
                  <TableHead>To Status</TableHead>
                  <TableHead>Avg Duration</TableHead>
                  <TableHead>P95</TableHead>
                  <TableHead>P99</TableHead>
                  <TableHead className="text-right">Samples</TableHead>
                  <TableHead className="w-32">Relative</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {metrics.map((m) => {
                  const isBottleneck =
                    m.avgDurationMs === maxAvgDuration && maxAvgDuration > 0;
                  return (
                    <TableRow
                      key={m.id}
                      className={isBottleneck ? 'bg-yellow-50' : ''}
                    >
                      <TableCell>
                        <Badge variant="outline">{m.fromStatus}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline">{m.toStatus}</Badge>
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {formatDuration(m.avgDurationMs)}
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {formatDuration(m.p95DurationMs)}
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {formatDuration(m.p99DurationMs)}
                      </TableCell>
                      <TableCell className="text-right">{m.sampleCount}</TableCell>
                      <TableCell>
                        <div className="h-2 w-full rounded-full bg-gray-100">
                          <div
                            className={`h-full rounded-full ${isBottleneck ? 'bg-yellow-500' : 'bg-blue-500'}`}
                            style={{
                              width: `${(m.avgDurationMs / maxAvgDuration) * 100}%`,
                            }}
                          />
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}

export function IntelligenceDashboardPage() {
  const [activeTab, setActiveTab] = useState('anomalies');

  return (
    <div className="space-y-6">
      <PageHeader
        title="Intelligence Dashboard"
        description="AI-powered anomaly detection, forecasting, error resolution, and process mining"
      />

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="anomalies">Anomalies</TabsTrigger>
          <TabsTrigger value="forecasts">Forecasts</TabsTrigger>
          <TabsTrigger value="error-resolution">Error Resolution</TabsTrigger>
          <TabsTrigger value="process-mining">Process Mining</TabsTrigger>
        </TabsList>

        <TabsContent value="anomalies">
          <AnomaliesTab />
        </TabsContent>

        <TabsContent value="forecasts">
          <ForecastsTab />
        </TabsContent>

        <TabsContent value="error-resolution">
          <ErrorResolutionTab />
        </TabsContent>

        <TabsContent value="process-mining">
          <ProcessMiningTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}
