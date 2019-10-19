var GET_PAGE_DATA = 1;
var GET_COMMIT_FILES = 2;
var GET_FILE_CONTENT = 3;

$(function() { // onload...do
    var data = "reqType="+ GET_PAGE_DATA;
    $.ajax({
        method: 'GET',
        url: "repo",
        dataType: 'json',
        data : data,
        timeout: 4000,
        error: function (e) {
            console.log(e.responseText);
        },
        success: function (r) {
            var branches = r.branches;
            var commits = r.commits;
            var wc = r.wcFiles;
            showBranchesData(branches);
            showCommits(commits);
            showWCFiles(wc);
        }
    })
});

function showBranchesData(branches){
    var branchesList = $('#branchesList');
    branchesList.empty();
    for (var i = 0 ; i < branches.length; i++) {
        var branch = branches[i];
        if(branch.isHead){
            branchesList.append("<li class='list-group-item list-group-item-primary'><h6>" + branch.name + "  ||  Head Branch</h6><p>" + branch.commitSha1 + "</p>" +
                "<input id=\"deleteBranch\" type=\"submit\" class=\"btn btn-primary pull-right\" value=\"Delete\" style=\"float: right;\"></li>");
        }
        else{
            branchesList.append("<li class='list-group-item list-group-item-light'><h6>" + branch.name + "</h6><p>" + branch.commitSha1 + "</p>" +
                "<input id=\"deleteBranch\" type=\"submit\" class=\"btn btn-primary pull-right\" value=\"Delete\" style=\"float: right;\"></li>");
        }
    }
}

function showWCFiles(files){
    var wcFileList = $('#wcFileList');
    wcFileList.empty();
    for (var i = 0 ; i < files.length; i++) {
        var file = files[i];
        if (!file.type.localeCompare("FILE")) {
            wcFileList.append("<button type=\"button\" class='list-group-item list-group-item-action'" +
                " data-toggle=\"modal\" data-target=\"#fileContentModal\" id=" + file.sha1 + ">" +
                "<h6>" + file.name + "</h6><p>"
                + file.sha1 + "</p></button>");
        }
    }
}

function showCommitFiles(files) {
    var table = document.getElementById("commitFilesTable");
    $("#commitFilesTable").find("tbody").empty();
    var tableBody = "";
    tableBody = tableBody + "<tr>";
    files.forEach(function(row){
            tableBody = tableBody + "<tr>";
            for (var j = 0; j < table.rows.length; j++){
                tableBody = tableBody + "<td><b>Name </b>" + row["name"] + "<br><b>Type </b>" +
                row["type"] + "<br><b>SHA1 </b>" + row["sha1"] + "<br><b>Modified By </b>" + row["lastModifier"] +
                    "<br><b>Modification Date </b>" + row["modificationDate"] + "</td>";
            }
            tableBody = tableBody + "</tr>";
        }
    );
    $('#commitFilesTable tbody').append(tableBody);
}


function showCommits(commits){
    var tableBody = "";
    tableBody = tableBody + "<tr>";
    var table = document.getElementById("commitsTable");

    // Create the data rows.
// Create the data rows.
    commits.forEach(function(row) {
        // Create a new row in the table for every element in the data array.
        tableBody = tableBody + "<tr style=\"cursor:pointer\">";

        for (var j = 0; j < table.rows.length; j++){
            tableBody = tableBody + "<td>" + row["sha1"] + "</td>";
            tableBody = tableBody + "<td>" + row["commit"]["message"] + "</td>";
            tableBody = tableBody + "<td>" + row["commit"]["creationDate"] + "</td>";
            tableBody = tableBody + "<td>" + row["commit"]["createdBy"] + "</td>";
            tableBody = tableBody + "<td>" + row["pointingBranches"] + "</td>";
        }

        tableBody = tableBody + "</tr>";
    });

    $('#commitsTable tbody').append(tableBody);

    for (var i = 1; i < table.rows.length; i++) {
        for (var j = 0; j < table.rows[i].cells.length; j++) {
            table.rows[i].onclick = function (e) {
                var commitSha1 = e.currentTarget.firstChild.innerText;
                var data = "reqType=" + GET_COMMIT_FILES + "&commitSha1=" + commitSha1;
                $.ajax({
                    method: 'GET',
                    data: data,
                    url: "repo",
                    processData: false, // Don't process the files
                    contentType: false, // Set content type to false as jQuery will tell the server its a query string request
                    timeout: 4000,
                    error: function (e) {
                        alert(e.responseText);
                    },
                    success: function (files) {
                        showCommitFiles(files);
                    }
                });
            }
        }
    }
}

$(document).ready(function(){
    $(document).on('click', '.list-group', function(e) {
        var data = "reqType=" + GET_FILE_CONTENT + "&commitSha1=" + e.target.innerHTML;
        $.ajax({
            url: 'repo',
            type: 'GET',
            data: data,
            success: function (response) {
                // Add response in Modal body
                $('.modal-body').html(response);

                // Display Modal
                $('#empModal').modal('show');
            }
        });
    });
});

//         $(document).ready(function(){
//     $("#fileContentModal").on('show.bs.modal', function(){
//
//     });
// });

