package cmd

import (
	"bufio"
	"fmt"
	"log"
	"os"

	"github.com/jtanza/bobolink/internal"
	"github.com/spf13/cobra"
)

var (
	dryRun   bool
	filePath string
)

func init() {
	var add = &cobra.Command{
		Use:   "add",
		Short: "Adds urls to store",
		Run: func(cmd *cobra.Command, args []string) {
			if len(args) == 0 {
				fmt.Printf("Please provide a URL to index.\n\n")
				cmd.Help()
			}

			if dryRun {
				docs, err := internal.Convert(args)
				if err != nil {
					log.Fatal(err)
				}
				for _, d := range docs {
					fmt.Printf("URL: %s\nText: %s\n", d.URL, d.Body)
				}
				return
			}

			var resources []string
			if filePath != "" {
				resources = resourcesFromFile(filePath)
			} else {
				resources = args
			}

			s := internal.NewSearch(indexPath)
			if _, err := s.AddResources(resources); err != nil {
				log.Fatal(err)
			}
		},
	}

	add.Flags().StringVarP(&filePath, "file", "f", "", "path to file of newline seperated links to index")
	add.Flags().BoolVarP(&dryRun, "dry-run", "d", false, "dry run. print what would be added to stdout")
	root.AddCommand(add)
}

func resourcesFromFile(path string) []string {
	file, err := os.Open(path)
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	res := make([]string, 0)
	for scanner.Scan() {
		res = append(res, scanner.Text())
	}
	return res
}
