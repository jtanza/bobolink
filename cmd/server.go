package cmd

import (
	"github.com/jtanza/bobolink/internal/api"
	"github.com/spf13/cobra"
	"log"
	"net/http"
)

func init() {
	var serverCmd = &cobra.Command{
		Use:   "server",
		Short: "starts the bobolink web server",
		Run: func(cmd *cobra.Command, args []string) {
			http.HandleFunc("/links/add", api.Add)
			http.HandleFunc("/links/find", api.Find)
			http.HandleFunc("/links/all", api.All)
			log.Fatal(http.ListenAndServe(":8080", nil))
		},
	}
	rootCmd.AddCommand(serverCmd)
}