import { Alert, List, Text } from "@mantine/core";
import { IconInfoCircle } from "@tabler/icons-react";
import type { AIRiskRecommendationData } from "../../shared/types";

interface AIRiskRecommendationProps {
  data: AIRiskRecommendationData;
}

export function AIRiskRecommendation({ data }: AIRiskRecommendationProps) {
  return (
    <Alert
      icon={<IconInfoCircle size={24} />}
      title={data.title}
      color="blue"
      variant="light"
      mb="xl"
      styles={{
        root: {
          backgroundColor: "#fff",
          borderColor: "#eef2f6",
          borderWidth: "1px",
          borderRadius: "8px",
        },
        title: {
          fontWeight: 600,
          marginBottom: "12px",
          fontSize: "15px",
        },
        body: {
          paddingTop: "6px",
        },
      }}
    >
      <List size="sm" spacing="xs" icon={<IconInfoCircle size={16} />}>
        {data.recommendations.map((recommendation, index) => (
          <List.Item key={index}>
            <Text size="sm">{recommendation}</Text>
          </List.Item>
        ))}
      </List>
    </Alert>
  );
}
